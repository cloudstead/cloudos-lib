require 'tempfile'
require 'fileutils'
require 'digest'

class Chef::Recipe::Postgresql

  PSQL = 'psql --quiet --tuples-only'
  PSQL_COMMAND = "#{PSQL} --command"

  def self.set_config (chef, group, key, value)
    pgsql_conf = %x(find /etc/postgresql/ -type f -name postgresql.conf)
    chef.bash "set #{key}=#{value} in #{pgsql_conf}" do
      user "root"
      code <<-EOH
alreadySet=$(grep "#{key}" #{pgsql_conf} | grep -v '^#' | wc -l)
if $alreadySet -gt 0 ; then
  tmp=$(mktemp postgresql.set_config.XXXXXXX) || die "error creating temp file"
  cat #{pgsql_conf} | grep -v "#{key}" > ${tmp}
  cat ${tmp} > #{pgsql_conf}
  rm ${tmp}
fi

echo "

#{key}=#{value}
" >> #{pgsql_conf}
      EOH
    end
  end

  def self.create_user (chef, dbuser, dbpass, allow_create_db = false)
    chef.bash "create pgsql user #{dbuser} at #{Time.now}" do
      user 'postgres'
      code <<-EOF
EXISTS=$(#{PSQL_COMMAND} "select usename from pg_user" | grep #{dbuser} | wc -l | tr -d ' ')
if [ ${EXISTS} -gt 0 ] ; then
  echo "User already exists: #{dbuser}"
else
  createuser #{allow_create_db ? '--create-db' : '--no-createdb'} --no-createrole --no-superuser #{dbuser}
fi
      EOF
    end
    chef.bash "set password for pgsql user #{dbuser}" do
      user 'postgres'
      code <<-EOF
echo "ALTER USER #{dbuser} PASSWORD '#{dbpass}'" | #{PSQL} -U postgres
      EOF
    end
  end

  def self.drop_user (chef, dbuser)
    chef.bash "dropping pgsql user #{dbuser}" do
      user 'postgres'
      code <<-EOF
dropuser #{dbuser}
      EOF
      not_if { %x(su - postgres bash -c '#{PSQL_COMMAND} "select usename from pg_user"').lines.grep(/#{dbuser}/).size == 0 }
    end
  end

  def self.db_exists(dbname)
    %x(su - postgres bash -c '#{PSQL_COMMAND} "select datname from pg_database"').lines.grep(/#{dbname}/).size > 0
  end

  def self.find_matching_databases (match)
    %x(sudo -u postgres -H bash -c "echo 'select datname from pg_database' | #{PSQL_COMMAND} template1").lines.grep(/#{match}/).collect { |db| db.chop }
  end

  def self.create_db (chef, dbname, dbuser = 'postgres', force = false)
    chef.bash "create pgsql database #{dbname} at #{Time.now}" do
      user 'postgres'
      code <<-EOF
EXISTS=$(#{PSQL_COMMAND} "select datname from pg_database" | grep #{dbname} | wc -l | tr -d ' ')
if [ ${EXISTS} -gt 0 ] ; then
  if [ "#{force}" = "true" ] ; then
    dropdb #{dbname}
    createdb --encoding=UNICODE --owner=#{dbuser} #{dbname}
  else
    echo "Database already exists: #{dbname}"
  fi
else
  createdb --encoding=UNICODE --owner=#{dbuser} #{dbname}
fi
EOF
    end
  end

  def self.count_tables(dbname, dbuser, dbpass)
    %x(su - postgres bash -c 'PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -c "select tableowner from pg_tables" #{dbname}').lines.grep(/#{dbuser}/).size
  end

  def self.has_tables(dbname, dbuser, dbpass)
    count_tables(dbname, dbuser, dbpass) > 0
  end

  def self.table_exists(dbname, dbuser, dbpass, tablename)
    if tablename
      %x(su - postgres bash -c 'PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -c "select * from pg_tables where tableowner=\\'#{dbuser}\\' and tablename=\\'#{tablename}\\'" #{dbname}').lines.grep(/#{tablename}/).size > 0
    else
      count_tables(dbname, dbuser, dbpass) > 0
    end
  end

  def self.initialize_db(chef, file, dbuser, dbpass, dbname, tablename = nil)
    lib = self
    set_script_perms(chef, dbname, file)
    chef.bash "initialize #{dbname} DB schema: #{file}" do
      user 'postgres'
      code <<-EOF
PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -f #{file} #{dbname}
      EOF
      not_if { lib.table_exists(dbname, dbuser, dbpass, tablename) }
    end
  end

  def self.insert(chef, dbuser, dbname, insert)

    base_lib = Chef::Recipe::Base
    sha = Digest::SHA256
    hash = "#{sha.hexdigest(insert[:sql])}_#{sha.hexdigest(insert[:unless])}"

    sql = "/tmp/sql_insert_#{hash}.sql"
    File.open(sql, "w") do |f|
      f.write(insert[:sql])
    end
    base_lib.set_perms(chef, sql, 'postgres', '600')

    check_sql = "/tmp/check_sql_#{hash}.sql"
    File.open(check_sql, "w") do |f|
      f.write(insert[:unless])
    end
    base_lib.set_perms(chef, check_sql, 'postgres', '600')

    chef.bash "insert into #{dbname} DB schema: #{insert[:sql]}" do
      user 'postgres'
      code <<-EOF
found=$(cat #{check_sql} | #{PSQL} #{dbname} | tr -d [:blank:])
if [[ -z "${found}" || ${found} -eq 0 ]] ; then
  output=$(cat #{sql} | #{PSQL} #{dbname} 2>&1)
  errors=$(echo -n "${output}" | grep ERROR | wc -l | tr -d ' ')
  if [ $errors -gt 0 ] ; then
    echo "Error inserting sql: #{%x(cat #{sql}).strip}: ${output}"
    exit 1
  fi
fi
      EOF
      not_if { %x(chown postgres #{check_sql} && su - postgres bash -c "cat #{check_sql} | #{PSQL} #{dbname}").strip.to_i > 0 }
    end
  end

  def self.run_script(chef, script, dbuser, dbpass, dbname)
    set_script_perms(chef, dbname, script)
    chef.bash "running #{script} against #{dbname} " do
      user 'postgres'
      code <<-EOF
PGPASSWORD=#{dbpass} #{PSQL} -U #{dbuser} -h 127.0.0.1 -f #{script} #{dbname}
      EOF
    end
  end

  def self.set_script_perms(chef, dbname, script)
    chef.bash "setting permissions on #{script} to run against #{dbname} " do
      user 'root'
      code <<-EOF
chown postgres #{script}
chmod 600 #{script}
      EOF
    end
  end

  def self.restart(chef)
    chef.bash 'restart Postgresql' do
      user 'root'
      code <<-EOH
if [[ "$(service mysql status)" =~ "down" ]] ; then
  service postgresql start
else
  service postgresql reload
fi
      EOH
    end
  end

  def self.dump(chef, dbname, dumpfile, dbuser = 'postgres')
    chef.bash "dumping #{dbname} to file #{dumpfile} " do
      user 'root'
      code <<-EOF
mkdir -p $(dirname #{dumpfile})
touch #{dumpfile}
chown #{dbuser} #{dumpfile}
sudo -u #{dbuser} -H pg_dump #{dbname} > #{dumpfile}
      EOF
    end
  end

  def self.drop_db (chef, dbname)
    exists = db_exists dbname
    chef.bash "dropping pgsql database #{dbname} at #{Time.now}" do
      user 'postgres'
      code <<-EOF
dropdb #{dbname}
      EOF
      only_if { exists }
    end
  end

  def self.create_metadata_table(chef, schema_name, schema_version, dbname, dbuser, dbpass)
    lib = self
    chef.bash "initialize #{dbname} DB schema metadata with version #{schema_name}/#{schema_version}" do
      user 'postgres'
      code <<-EOF
echo "
CREATE TABLE __cloudos_metadata__ (m_category varchar(255), m_name varchar(255), m_value varchar(255), m_time timestamp with time zone);
INSERT INTO __cloudos_metadata__ (m_category, m_name, m_value, m_time) VALUES ('schema_version', '#{schema_name}', '#{schema_version}', now());
" \
  | PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 --single-transaction #{dbname}
EOF
      not_if { lib.table_exists(dbname, dbuser, dbpass, '__cloudos_metadata__') }
    end
  end

  def self.update_schema(chef, schema_name, schema_version, schema_file, dbname, dbuser, dbpass)
    chef.bash "update #{dbname} DB schema metadata with version: #{schema_name}/#{schema_version}" do
      user 'root'
      code <<-EOF
temp=$(mktemp /tmp/update_schema.XXXXXX.sql)
echo "-- SQL file: #{schema_file}" >> ${temp} && \
cat #{schema_file} >> ${temp} && \
echo "
INSERT INTO __cloudos_metadata__ (m_category, m_name, m_value, m_time) VALUES ('schema_version', '#{schema_name}', '#{schema_version}', now());
" >> ${temp} && \
PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -f ${temp} --single-transaction #{dbname}

EOF
    end
  end

  def self.get_schema_version(chef, dbname, dbuser, dbpass, schema_name)
    %x(echo "SELECT m_value FROM __cloudos_metadata__ WHERE m_category='schema_version' AND m_name='#{schema_name}' ORDER BY m_time DESC LIMIT 1" | PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 #{dbname}).strip
  end

end
