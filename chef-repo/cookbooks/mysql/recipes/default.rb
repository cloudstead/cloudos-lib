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

# set sql_mode=STRICT_ALL_TABLES in /etc/mysql/my.cnf


# elsewhere: set date.timezone=America/Los_Angeles in /etc/php5/apache2/php.ini
