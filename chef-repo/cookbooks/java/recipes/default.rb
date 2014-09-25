#
# Cookbook Name:: java
# Recipe:: default
#
# Copyright 2014, cloudstead
#
# All rights reserved - Do Not Redistribute
#
package 'openjdk-7-jre-headless' do
  action :install
end

%w( jrun jrun-init ).each do |file|
  cookbook_file "/usr/local/bin/#{file}" do
    owner 'root'
    group 'root'
    mode '0755'
    action :create
  end
end

startcom_ca_cert_name='StartComClass2PrimaryIntermediateServerCA'
startcom_ca_cert="/usr/share/ca-certificates/mozilla/#{startcom_ca_cert_name}.crt"

java = Chef::Recipe::Java
java.install_cert self, startcom_ca_cert_name, startcom_ca_cert
