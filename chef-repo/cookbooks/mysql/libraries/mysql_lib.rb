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

  def self.drop_user (chef, dbuser)
    chef.bash "dropping mysql user #{dbuser}" do
      user 'root'
      code <<-EOF
echo "DELETE USER '#{dbuser}'" | mysql -u root
      EOF
      not_if { %x(echo "select count(*) from mysql.user where User='#{dbuser}'" | mysql -s -u root).to_i == 0 }
    end
  end

  def self.db_exists (dbname)
    %x(echo "show databases" | mysql -s -u root).lines.grep(/#{dbname}/).size > 0
  end

  def self.create_db (chef, dbname, dbuser)
    lib = self
    chef.bash "create mysql database #{dbname}" do
      user 'root'
      code <<-EOF
echo "CREATE DATABASE #{dbname}" | mysql -u root && \
echo "GRANT ALL ON #{dbname}.* TO '#{dbuser}'" | mysql -u root
      EOF
      not_if { lib.db_exists dbname }
    end
  end

  def self.count_tables(dbname, dbuser, dbpass)
    %x(echo "show tables" | mysql -s -u #{dbuser} #{dbname}).lines.grep(/#{dbname}/).size
  end

  def self.table_exists(dbname, dbuser, dbpass, tablename)
    if tablename
      %x(echo "show tables" | mysql -s -u #{dbuser} -p#{dbpass} #{dbname} | grep #{tablename}).lines.grep(/#{tablename}/).size > 0
    else
      %x(echo "show tables" | mysql -s -u #{dbuser} -p#{dbpass} #{dbname} | wc -l).strip.to_i > 0
    end
  end

  def self.initialize_db(chef, file, dbuser, dbpass, dbname, tablename = nil)
    run_script chef, file, dbuser, dbpass, dbname unless table_exists(dbname, dbuser, dbpass, tablename)
  end

  def self.run_script(chef, script, dbuser, dbpass, dbname)
    chef.bash "running #{script} against #{dbname} " do
      user 'root'
      code <<-EOF
cat #{script} | mysql -u #{dbuser} -p#{dbpass} #{dbname}
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

    mysql="mysql -s -u root #{dbname}"

    chef.bash "insert into #{dbname} DB schema: #{insert[:sql]}" do
      user 'root'
      code <<-EOF
found=$(cat #{check_sql} | #{mysql} | tr -d [:blank:])
if [[ -z "${found}" || ${found} -eq 0 ]] ; then
  output=$(cat #{sql} | #{mysql} 2>&1)
  errors=$(echo -n ${output} | grep ERROR | wc -l | tr -d ' ')
  if [ $errors -gt 0 ] ; then
    echo "Error inserting sql: #{%x(cat #{sql}).strip}: ${output}"
    exit 1
  fi
fi
      EOF
      not_if { %x(cat #{check_sql} | #{mysql}").strip.to_i > 0 }
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

  def self.dump(chef, dbname, dbuser='root', dumpfile)
    chef.bash "dumping #{dbname} to #{dumpfile} " do
      user dbuser
      code <<-EOF
mysqldump #{dbname} > #{dumpfile}
      EOF
    end
  end

  def self.drop_db (chef, dbname, dbuser = 'root', dbpass = nil)
    chef.bash "dropping mysql database #{dbname}" do
      user 'root'
      code <<-EOF
mysqladmin -u #{dbuser} #{dbpass ? "-p #{dbpass}" : ''} drop #{dbname}
      EOF
    end
  end

  def self.create_metadata_table(chef, schema_name, schema_version, dbname, dbuser, dbpass)
    lib = self
    chef.bash "initialize #{dbname} DB schema metadata with version #{schema_name}/#{schema_version}" do
      user 'root'
      code <<-EOF
echo "
CREATE TABLE __cloudos_metadata__ (m_category varchar(255), m_name varchar(255), m_value varchar(255), m_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);
INSERT INTO __cloudos_metadata__ (m_category, m_name, m_value) VALUES ('schema_version', '#{schema_name}', '#{schema_version}');
" \
  | mysql -u root #{dbname}
      EOF
      not_if { lib.table_exists(dbname, dbuser, dbpass, '__cloudos_metadata__') }
    end
  end

  def self.update_schema(chef, schema_name, schema_version, schema_file, dbname, dbuser, dbpass)
    chef.bash "update #{dbname} DB schema metadata with version: #{schema_name}/#{schema_version}" do
      user 'root'
      code <<-EOF
temp=$(mktemp /tmp/update_schema.XXXXXX.sql)
echo "START TRANSACTION WITH CONSISTENT SNAPSHOT; SET autocommit=0;" >> ${temp}
cat #{schema_file} >> ${temp}
echo "
INSERT INTO __cloudos_metadata__ (m_category, m_name, m_value) VALUES ('schema_version', '#{schema_name}', '#{schema_version}');
" >> ${temp}
echo "COMMIT;" >> ${temp}

cat ${temp} | mysql -u root #{dbname}
      EOF
    end
  end

  def self.get_schema_version(chef, dbname, dbuser, dbpass, schema_name)
    %x(echo "SELECT m_value FROM __cloudos_metadata__ WHERE m_category='schema_version' AND m_name='#{schema_name}' ORDER BY m_time DESC LIMIT 1" | mysql -u root #{dbname}).strip
  end

end