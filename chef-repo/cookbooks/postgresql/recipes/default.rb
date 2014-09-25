#
# Cookbook Name:: postgresql
# Recipe:: default
#
# Copyright 2013, cloudstead
#
# All rights reserved - Do Not Redistribute
#

%w( postgresql-9.3 ).each do |pkg|
  package pkg do
    action :install
  end
end
