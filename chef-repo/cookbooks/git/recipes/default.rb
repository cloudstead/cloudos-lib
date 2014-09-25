#
# Cookbook Name:: cloudos
# Recipe:: default
#
# Copyright 2013, cloudstead
#
# All rights reserved - Do Not Redistribute
#

%w( git ).each do |pkg|
  package pkg do
    action :install
  end
end

