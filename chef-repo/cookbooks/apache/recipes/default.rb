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
