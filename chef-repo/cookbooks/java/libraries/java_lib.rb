class Chef::Recipe::Java

  def self.java_home
    java=%x(which java).strip
    return java.empty? ?
        nil :
        %x(ls -l $(ls -l $(ls -l $(ls -l #{java} | awk '{print $NF}') | awk '{print $NF}') | awk '{print $NF}') | awk '{print $NF}' | sed -e 's,/jre/bin/java,,').strip
  end

  def self.ca_certs
    "#{self.java_home}/jre/lib/security/cacerts"
  end

  def self.install_cert(chef, name, pem_path)
    cacerts = self.ca_certs
    chef.bash "Copy SSL certificate #{pem_path} to Java Key Store" do
      user 'root'
      cwd '/tmp'
      code <<-EOH
echo "changeit
yes" | keytool -import -alias #{name.downcase} -keypass changeit -keystore #{cacerts} -file #{pem_path}
      EOH
      not_if { %x( echo "changeit" | keytool -list -v -keystore #{cacerts}).lines.grep(/#{name.downcase}/).size > 0 }
    end
  end

  def self.declare_service (base_dir, java_class = '', config = '')

    svc = Pathname.new(base_dir).basename.to_s

    if java_class.nil? || java_class.strip.empty?
      proc_pattern = "-jar (.*/)?target/#{svc}-"
    else
      simple_classname = java_class.partition('.').last
      if config.nil? || config.strip.empty?
        proc_pattern = java_class
        svc = "#{svc}-#{simple_classname}"
      else
        proc_pattern = "#{java_class} #{config}"
        config_basename = %x(basename #{config}).strip.partition('.').first
        svc = "#{svc}-#{simple_classname}-#{config_basename}"
      end
    end

    { :service_name => svc, :proc_pattern => proc_pattern }
  end

  def self.create_service(chef, base_dir, run_as_user, java_class = '', config = '')
    svc = declare_service(base_dir, java_class, config)
    chef.template svc[:service_name] do
      path "/etc/init.d/#{svc[:service_name]}"
      source 'java-service-init.erb'
      owner 'root'
      group 'root'
      mode '0755'
      cookbook 'java'
      variables(
          :app_dir => base_dir,
          :run_as_user => run_as_user,
          :java_class => java_class,
          :config => config
      )
      action :create
    end

    chef.service svc[:service_name] do
      pattern svc[:proc_pattern]
      supports [ :start => true, :stop => true, :restart => true, :status => true ]
      action [ :enable, :start ]
    end
  end

end
