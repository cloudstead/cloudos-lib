require 'securerandom'
require 'json'

class Chef::Recipe::Base

  @CLIENT_SECRET = '/etc/.cloudos'

  def self.chef_user
    %x(sudo cat /etc/chef-user).strip
  end

  def self.user_home(user)
    %x(echo $(bash -c "cd ~#{user} && pwd")).strip
  end

  def self.chef_dir
    File.absolute_path("#{File.dirname(__FILE__)}/../../..")
  end

  def self.chef_files(cookbook)
    "#{chef_dir}/cookbooks/#{cookbook}/files/default"
  end

  def self.chef_templates(cookbook)
    "#{chef_dir}/cookbooks/#{cookbook}/templates/default"
  end

  def self.chef_databags(cookbook)
    "#{chef_dir}/data_bags/#{cookbook}"
  end

  def self.chef_user_home
    user_home(chef_user)
  end

  def self.ports_databag(chef, app)
    ports = nil
    begin
      ports = chef.data_bag_item(app, 'ports')
    rescue => e
      puts "No ports databag found for #{app}, creating one"
      ports = { 'id' => 'ports', 'primary' => 'pick', 'admin' => 'pick' }
    end

    writeback = false
    if ports['primary'] == 'pick'
      ports['primary'] = pick_port
      writeback = true
    end
    if ports['admin'] == 'pick'
      ports['admin'] = pick_port
      writeback = true
    end

    if writeback
      file = "#{chef_databags(app)}/ports.json"
      File.open(file, 'w') {|f| f.write(ports.to_json) }
      %x(chown #{chef_user} #{file} && chmod 600 #{file})
    end

    # Now this had better work...
    chef.data_bag_item(app, 'ports')
  end

  def self.pick_port
    port = 5000 + rand(26000)
    until port_available(port)
      port += 1
      break if port > 32768
    end
    raise 'Could not find an open port!' if port > 32768
    port
  end

  def self.port_available(port)
    raise 'port_available: port was nil or empty' if port.to_s.empty?
    raise "invalid port: #{port}" unless port.to_s =~ /^\d+$/
    port_check_output=%x(
      port=#{port}
      for addr in $(ifconfig | grep 'inet addr' | tr ':' ' ' | awk '{print $3}') ; do
        if nc -z ${addr} ${port} ; then
          echo "listening on ${addr}"
        fi
      done
    )
    return false if port_check_output.to_s.include? 'listening'

    %w(primary admin).each do |type|
      assigned=%x(find #{chef_dir}/data_bags -type f -name ports.json | xargs egrep '"#{type}"[[:space:]]*:[[:space:]]*#{port}')
      return false unless assigned.to_s.strip.empty?
    end

    true
  end

  def self.secret
    if ! File.exists? @CLIENT_SECRET
      File.open(@CLIENT_SECRET, 'wb') { |f| f.write SecureRandom.hex(64) }
    end
    %x(chmod 700 #{@CLIENT_SECRET})

    file = File.open(@CLIENT_SECRET, 'rb')
    contents = file.read
    file.close

    contents
  end

  def self.password(service)
    Digest::SHA256.hexdigest("#{service}_#{secret}")
  end

  # Returns a random hexadecimal string of arbitrary size.
  def self.random_password(size=8)
    SecureRandom.hex[0..size]
  end

  def self.sha_file(file)
    Digest::SHA256.file(file).hexdigest
  end

  def self.logrotate(chef, path)
    chef.template path do
      source 'log_rotate.erb'
      owner 'root'
      group 'root'
      mode '0755'
      cookbook 'base'
      variables({ :path => path })
      action :create
    end
  end

  def self.gid(name)
    %x(cat /etc/group | grep "^#{name}:" | awk -F ':' '{print $(NF-1)}').strip
  end

  def self.uid(name)
    %x(id -u #{name}).strip
  end

  def self.install_ssl_cert(chef, app, name)
    # This is where deploy.sh puts the ssl certs provided in INIT_FILES directory
    ssl_pem_src = local_pem_path(app, name)
    ssl_key_src = "#{chef_user_home}/chef/certs/#{app}/#{name}.key"
    if File.exists?(ssl_pem_src) && File.exists?(ssl_key_src)

      ssl_pem_dest = "/etc/ssl/certs/#{name}.pem"
      ssl_key_dest = "/etc/ssl/private/#{name}.key"

      chef.bash "install SSL cert '#{name}' from #{ssl_pem_src}" do
        user 'root'
        cwd '/tmp'
        code <<-EOF
# use -cv so they are only copied if the checksum differs
rsync -cv #{ssl_pem_src} #{ssl_pem_dest} && chmod 644 #{ssl_pem_dest} && \
rsync -cv #{ssl_key_src} #{ssl_key_dest} && chmod 600 #{ssl_key_dest}
        EOF
        not_if { File.exists?(ssl_pem_dest) && File.exists?(ssl_key_dest) }
      end
    end
  end

  def self.local_pem_exists(app, name)
    File.exists? local_pem_path(app, name)
  end

  def self.local_pem_app_path (app)
    "#{chef_user_home}/chef/certs/#{app}"
  end

  def self.local_pem_path(app, name)
    "#{local_pem_app_path(app)}/#{name}.pem"
  end

  def self.local_certs(app)
    %x(for pem in $(find #{local_pem_app_path(app)} -type f -name "*.pem") ; do basename "${pem}" | sed -e 's/.pem$//' ; done).split
  end

  def self.local_pem_cn(app, name)
    pem_cn_path local_pem_path(app, name)
  end

  def self.pem_path(name)
    "/etc/ssl/certs/#{name}.pem"
  end

  def self.pem_cn(name)
    pem_cn_path pem_path(name)
  end

  def self.pem_cn_path(path)
    return nil if path.nil? || !File.exists?(path)

    pem_subject = %x(openssl x509 -in #{path} -subject | grep subject= | head -n 1).strip
    %x(echo 'For path #{path}, subject=#{pem_subject}' > /tmp/pem_cn_#{File.basename(path, '.pem')})
    matches = /\/CN=([^\/]+)\//.match(pem_subject)
    matches ? matches[1] : nil
  end

  def self.set_hostname(chef, fqdn)
    chef.bash "set hostname to #{fqdn}" do
      user 'root'
      cwd '/tmp'
      code <<-EOF
if [ $(hostname) != "#{fqdn}" ] ; then
  hostname #{fqdn}
  echo #{fqdn} > /etc/hostname
fi
      EOF
    end

    chef.template '/etc/hosts' do
      owner 'root'
      group 'root'
      mode '0644'
      cookbook 'base'
      variables({ :fqdn => fqdn })
      action :create
    end
  end

  def self.set_perms (chef, path, owner, perms)
    chef.bash "set_perms: owner=#{owner}, perms=#{perms} for path=#{path}" do
      user 'root'
      code <<-EOF
chown #{owner} #{path} || exit 1
chmod #{perms} #{path} || exit 1
EOF
    end
  end

  def self.remove_from_file (chef, file, data)
    chef.bash "remove #{data} from #{file}" do
      user 'root'
      cwd '/tmp'
      code <<-EOF
found=$(cat #{file} | grep -- "#{data}" | wc -l | tr -d ' ')
if [ $found ] ; then
  temp=$(mktemp /tmp/remove_from_file.XXXXXXX)
  cat #{file} | grep -v -- "#{data}" > ${temp}
  if [[ $(stat --printf="%s" ${temp} -gt 0 ]] ; then
    cp #{file} $(mktemp $(dirname #{file})/$(basename #{file}).$(date +%Y-%m-%d_%H-%M-%S).XXXXX) && \
    cat ${temp} > #{file} && rm -f ${temp}
  fi
fi
EOF
      not_if { File.readlines(file).grep(/Regexp.escape(data)/).empty? }
    end
  end

end