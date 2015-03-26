#
# Cookbook Name:: base
# Recipe:: default
#
# Copyright 2014, cloudstead
#

require 'securerandom'

# ensure packages are up to date
execute 'apt-get update' do
  command 'apt-get update'
end

# every system needs these
%w( openntpd safe-rm ).each do |pkg|
  package pkg do
    action :install
  end
end

# common utilities
%w( emacs24-nox tshark xtail screen ).each do |pkg|
  package pkg do
    action :install
  end
end

base = Chef::Recipe::Base
[ '/root/.screenrc', "#{base.chef_user_home}/.screenrc" ].each do |screenrc|
  cookbook_file screenrc do
    source 'dot-screenrc'
    owner 'root'
    group 'root'
    mode '0755'
    action :create
  end
end

startcom_ca_cert_name='StartComClass2PrimaryIntermediateServerCA'
startcom_ca_cert="/usr/share/ca-certificates/mozilla/#{startcom_ca_cert_name}.crt"
startcom_ca_cert_hash='5a5c01b6.0'
ca_cert_dir='/etc/ssl/certs'
cookbook_file startcom_ca_cert do
  source 'StartComClass2PrimaryIntermediateServerCA.pem'
  owner 'root'
  group 'root'
  mode '0644'
  action :create
end

bash 'install StartCom CA Cert' do
  user 'root'
  code <<-EOF
cd #{ca_cert_dir} && \
rm -f #{startcom_ca_cert_name}.pem && \
ln -s #{startcom_ca_cert} #{startcom_ca_cert_name}.pem && \
ln -s #{startcom_ca_cert_name}.pem #{startcom_ca_cert_hash}
  EOF
  not_if { File.exists? "#{ca_cert_dir}/#{startcom_ca_cert_hash}" }
end

base_lib = Chef::Recipe::Base
bash 'install data_files' do
  user 'root'
  code <<-EOF
DATA_DIR="/opt/cloudos"
DATA_FILES="#{base_lib.chef_dir}/data_files"
mkdir -p ${DATA_DIR} && chown root.root ${DATA_DIR} && chmod 755 ${DATA_DIR}
if [ $(find ${DATA_FILES} -type f 2> /dev/null | wc -l | tr -d ' ') -gt 0 ] ; then
  rsync -avzc ${DATA_FILES}/* ${DATA_DIR}/
  for f in $(find ${DATA_DIR} -type f -name "*.gz") ; do
    gunzip ${f}
  done
  chmod -R 755 ${DATA_DIR}
fi
EOF
end
