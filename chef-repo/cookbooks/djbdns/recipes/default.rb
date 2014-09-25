#
# Cookbook Name:: djbdns
# Recipe:: default
#
# Copyright 2014, cloudstead
#
# All rights reserved - Do Not Redistribute
#

%w( daemontools daemontools-run ucspi-tcp djbdns ).each do |pkg|
  package pkg do
    action :install
  end
end

user "dnslog" do
  shell "/bin/false"
end

user "tinydns" do
  shell "/bin/false"
end

bash "configure tinydns" do
  user "root"
  cwd "/tmp"
  code <<-EOF
tinydns-conf tinydns dnslog /etc/tinydns/ #{node[:ipaddress]}
mkdir /etc/service ; cd /etc/service ; ln -sf /etc/tinydns/
initctl start svscan
  EOF
  not_if { File.exists? "/etc/tinydns" }
end
