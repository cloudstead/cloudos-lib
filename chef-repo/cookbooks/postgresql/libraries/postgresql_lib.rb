require 'tempfile'
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
    chef.bash "create pgsql user #{dbuser}" do
      user 'postgres'
      code <<-EOF
createuser #{allow_create_db ? '--create-db' : '--no-createdb'} --no-createrole --no-superuser #{dbuser}
      EOF
      not_if { %x(su - postgres bash -c '#{PSQL_COMMAND} "select usename from pg_user"').lines.grep(/#{dbuser}/).size > 0 }
    end
    chef.bash "set password for pgsql user #{dbuser}" do
      user 'postgres'
      code <<-EOF
echo "ALTER USER #{dbuser} PASSWORD '#{dbpass}'" | #{PSQL} -U postgres
      EOF
    end
  end

  def self.create_db (chef, dbname, dbowner = 'postgres')
    chef.bash "create pgsql database #{dbname}" do
      user 'postgres'
      code <<-EOF
createdb --encoding=UNICODE --owner=#{dbowner} #{dbname}
      EOF
      not_if { %x(su - postgres bash -c '#{PSQL_COMMAND} "select datname from pg_database"').lines.grep(/#{dbname}/).size > 0 }
    end
  end

  def self.count_tables(dbname, dbuser, dbpass)
    %x(su - postgres bash -c 'PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -c "select tableowner from pg_tables" #{dbname}').lines.grep(/#{dbuser}/).size
  end

  def self.table_exists(dbname, dbuser, dbpass, tablename)
    %x(su - postgres bash -c 'PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -c "select * from pg_tables where tableowner=\\'#{dbuser}\\' and tablename=\\'#{tablename}\\'" #{dbname}').lines.grep(/#{tablename}/).size > 0
  end

  def self.initialize_db(chef, file, dbuser, dbpass, dbname, tablename = nil)
    set_script_perms(chef, dbname, file)
    chef.bash "initialize #{dbname} DB schema: #{file}" do
      user 'postgres'
      code <<-EOF
PGPASSWORD="#{dbpass}" #{PSQL} -U #{dbuser} -h 127.0.0.1 -f #{file} #{dbname}
      EOF
      not_if { tablename.nil? \
        ? Chef::Recipe::Postgresql.count_tables(dbname, dbuser, dbpass) > 0 \
        : Chef::Recipe::Postgresql.table_exists(dbname, dbuser, dbpass, tablename)
      }
    end
  end

  def self.insert(chef, dbuser, dbname, insert)

    sha = Digest::SHA256
    hash = "#{sha.hexdigest(insert[:sql])}_#{sha.hexdigest(insert[:unless])}"

    sql = "/tmp/sql_insert_#{hash}.sql"
    File.open(sql, "w") do |f|
      f.write(insert[:sql])
    end

    check_sql = "/tmp/check_sql_#{hash}.sql"
    File.open(check_sql, "w") do |f|
      f.write(insert[:unless])
    end

    chef.bash "insert into #{dbname} DB schema: #{insert[:sql]}" do
      user 'postgres'
      code <<-EOF
output=$(cat #{sql} | #{PSQL} #{dbname} 2>&1)
errors=$(echo -n ${output} | grep ERROR | wc -l | tr -d ' ')
if [ $errors -gt 0 ] ; then
  echo "Error inserting sql: #{%x(cat #{sql}).strip}: ${output}"
  exit 1
fi
      EOF
      not_if {
        %x(su - postgres bash -c "cat #{check_sql} | #{PSQL} #{dbname}").strip.to_i > 0
      }
    end

    # File.delete(sql)
    # File.delete(check_sql)
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

end
