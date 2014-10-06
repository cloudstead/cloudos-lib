class Chef::Recipe::Mysql

  def self.set_config (chef, group, key, value)
    mysql_cnf = "/etc/mysql/conf.d/#{group}__#{key}.cnf"
    chef.bash "set #{key}=#{value} via #{mysql_cnf}" do
      user "root"
      code <<-EOH
echo "[#{group}]
#{key}=#{value}" > #{mysql_cnf}
      EOH
    end
  end

  def self.create_user (chef, dbuser, dbpass, allow_create_db = nil)
    chef.bash "create mysql user #{dbuser}" do
      user 'root'
      code <<-EOF
echo "CREATE USER #{dbuser} IDENTIFIED BY '#{dbpass}'" | mysql -u root
      EOF
      not_if { %x(echo "select count(*) from mysql.user where User='#{dbuser}'" | mysql -s -u root).to_i > 0 }
    end
  end

  def self.create_db (chef, dbname)
    chef.bash "create mysql database #{dbname}" do
      user 'root'
      code <<-EOF
echo "CREATE DATABASE #{dbname}" | mysql -u root
      EOF
      not_if { %x(echo "show databases" | mysql -s -u root).lines.grep(/#{dbname}/).size > 0 }
    end
  end

  def self.count_tables(dbname, dbuser, dbpass)
    %x(echo "show tables" | mysql -s -u #{dbuser} #{dbname}).lines.grep(/#{dbname}/).size
  end

  def self.table_exists(dbname, dbuser, dbpass, tablename)
    %x(echo "show tables" | mysql -s -u #{dbuser} #{dbname} | grep #{tablename}).lines.grep(/#{tablename}/).size > 0
  end

  def self.initialize_db(chef, file, dbuser, dbpass, dbname, tablename = nil)
    run_script chef, file, dbuser, dbpass, dbname if tablename.nil? || !table_exists(dbname, dbuser, dbpass, tablename)
  end

  def self.run_script(chef, script, dbuser, dbpass, dbname)
    chef.bash "running #{script} against #{dbname} " do
      user 'root'
      code <<-EOF
cat #{script} | mysql -U #{dbuser} #{dbname}
      EOF
    end
  end

  def self.insert(chef, dbuser, dbname, insert)

    hash1 = Digest::SHA256.hexdigest(insert[:sql])
    hash2 = Digest::SHA256.hexdigest(insert[:unless])

    sql = "/tmp/sql_insert_#{hash1}.sql"
    File.open(sql, "w") do |f|
      f.write(insert[:sql])
    end

    check_sql = "/tmp/check_sql_#{hash2}.sql"
    File.open(check_sql, "w") do |f|
      f.write(insert[:unless])
    end

    chef.bash "insert into #{dbname} DB schema: #{insert[:sql]}" do
      user 'root'
      code <<-EOF
cat #{sql} | mysql -u #{dbuser} #{dbname} > #{sql}.out
      EOF
      not_if { %x(cat #{check_sql} | mysql -s -u #{dbuser} #{dbname}").strip.to_i > 0 }
    end
  end

  def self.restart(chef)
      chef.bash 'restart mysql' do
      user 'root'
      code <<-EOH
if [[ "$(service mysql status)" =~ "stop" ]] ; then
  service mysql start
else
  service mysql reload
fi
      EOH
    end
  end

end