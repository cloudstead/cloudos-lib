#
# Cookbook Name:: mysql
# Recipe:: default
#
# Copyright 2014, cloudstead
#
# All rights reserved - Do Not Redistribute
#

%w( mysql-server ).each do |pkg|
  package pkg do
    action :install
  end
end
