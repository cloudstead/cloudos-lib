#
# Cookbook Name:: apache
# Recipe:: default
#
# Copyright 2013, cloudstead
#
# All rights reserved - Do Not Redistribute
#

# every system needs these
%w( apache2 ).each do |pkg|
  package pkg do
    action :install
  end
end

%w( https-services-available https-services-enabled rewrite-rules-available rewrite-rules-enabled mixins ).each do |dir|
  directory "/etc/apache2/#{dir}" do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
  end
end

# overwrite ports with our version that sets a few defaults
%w( /etc/apache2/ports.conf ).each do |config|
  if File.exists? config
    template config do
      action :delete
    end
  end
  template config do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    variables ({
        :hostname => %x(hostname).strip,
        :ip_address => node['ipaddress'],
    })
  end
end

# default modules needed by most apps
%w( ssl rewrite headers proxy proxy_http include substitute setenvif ).each do |mod|
  Apache.enable_module(self, mod)
end

# Disable default sites. CloudOs will enable default-ssl on a new site name
Apache.disable_site(self, 'default-ssl')
Apache.disable_site(self, 'default')

# Enable GeoIP if one or more databases is provided
geo_db_dir = '/opt/cloudos/geoip'
unless %x(find #{geo_db_dir}/ -type f -name "Geo*2-*.mmdb").strip.empty?

  self.bash 'ensure maxmind PPA is setup and libmaxminddb is installed' do
    user 'root'
    code <<-EOF
if [ $(dpkg -l | grep libmaxminddb | wc -l | tr -d ' ') -eq 0 ] ; then
  echo | add-apt-repository ppa:maxmind/ppa
  apt-get update
  apt-get install libmaxminddb0 libmaxminddb-dev mmdb-bin -y
fi

# Hacky, but some things expect the legacy databases to have a certain name
if [[ -f #{geo_db_dir}/GeoLiteCity.dat && ! -e #{geo_db_dir}/GeoIPCity.dat ]] ; then
    ln -s #{geo_db_dir}/GeoLiteCity.dat #{geo_db_dir}/GeoIPCity.dat
fi

EOF
  end

  Apache.new_module(self, 'maxminddb')
  template '/etc/apache2/mixins/maxmind.conf' do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    variables ({ :geo_db_dir => geo_db_dir })
  end
end
