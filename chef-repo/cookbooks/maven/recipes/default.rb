#
# Cookbook Name:: maven
# Recipe:: default
#
# Author:: Seth Chisamore (<schisamo@opscode.com>)
# Author:: Bryan W. Berry (<bryan.berry@gmail.com>)
#
# Copyright:: 2010-2012, Opscode, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include_recipe 'java::default'

mvn_version = node['maven']['version'].to_s
mvn_full_version = node['maven'][mvn_version]['version']
mvn_tarball="mvn-#{mvn_version}.tar.bz2"
mvn_local_tarball="#{Chef::Config[:file_cache_path]}/#{mvn_tarball}"
mvn_binary_path = '/usr/local/maven/bin/mvn'

unless File.exists? mvn_binary_path
  remote_file mvn_local_tarball do
    source node['maven'][mvn_version]['url']
  end
end

bash "install maven" do
  user "root"
  cwd "/tmp"
  code <<-EOH
rm -rf /usr/local/maven* /usr/local/apache-maven*
cd /usr/local && tar xzf #{mvn_local_tarball}
ln -sf /usr/local/apache-maven-#{mvn_full_version} /usr/local/maven
ln -sf /usr/local/maven/bin/mvn /usr/local/bin/mvn
  EOH
  not_if { File.exists? mvn_binary_path }
end

template '/etc/mavenrc' do
  source 'mavenrc.erb'
  mode   '0755'
end
