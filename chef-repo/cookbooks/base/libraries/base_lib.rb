require 'securerandom'

class Chef::Recipe::Base

  @CLIENT_SECRET = '/etc/.cloudos'

  def self.chef_user
    %x(sudo cat /etc/chef-user).strip
  end

  def self.user_home(user)
    %x(echo $(bash -c "cd ~#{user} && pwd")).strip
  end

  def self.chef_user_home
    user_home(chef_user)
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

end