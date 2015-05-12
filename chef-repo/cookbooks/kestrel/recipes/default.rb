#
# Cookbook Name:: kestrel
# Recipe:: default
#
# Copyright 2012, cloudstead.io
#
# All rights reserved - Do Not Redistribute
#

include_recipe 'java::jdk'

package 'daemon'
package 'unzip'

kestrel_user = 'kestrel'
kestrel_home = '/usr/local/kestrel'

user kestrel_user do
  home kestrel_home
  shell '/bin/bash'
  system true
end

%w(/usr/local /var/log /var/run /var/spool).each do |dir|
  directory "#{dir}/kestrel" do
    owner kestrel_user
    action :create
    mode 0755
  end
end

template '/etc/kestrel.env' do
  owner kestrel_user
  mode '0644'
  action :create_if_missing
end

remote_file '/tmp/kestrel-2.4.1.zip' do
  action :create_if_missing
  source 'http://cloudstead.io/downloads/kestrel-2.4.1.zip'
end

bash 'install kestrel' do
  cwd kestrel_home
  code <<-EOH
unzip /tmp/kestrel-2.4.1.zip
ln -s kestrel-2.4.1 current
chmod +x current/scripts/*

# Make it look like a regular jrun service so we can start/stop/status it with jrun-init
mkdir -p #{kestrel_home}/logs
mkdir -p #{kestrel_home}/target
cd #{kestrel_home}/target
KESTREL_JAR=$(find ../current/ -type f -name "kestrel*.jar" | grep -v javadoc | grep -v sources | grep -v test)
if [ -z ${KESTREL_JAR} ] ; then
  echo "Kestrel jar not found"
  exit 1
fi
ln -s ${KESTREL_JAR} kestrel-$(basename ${KESTREL_JAR})
ln -s ../current/config
cd #{kestrel_home}

chown -R #{kestrel_user} #{kestrel_home}
  EOH
end

Chef::Recipe::Java.create_service self, kestrel_home, kestrel_user
