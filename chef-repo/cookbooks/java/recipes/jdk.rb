#
# Cookbook Name:: java
# Recipe:: jdk
#
# Copyright 2014, cloudstead
#
# All rights reserved - Do Not Redistribute
#

%w( openjdk-7-jdk ).each do |pkg|
  package pkg do
    action :install
  end
end

