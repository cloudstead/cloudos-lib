#!/bin/bash
#
# gen-sql.sh <dbname> <sql-file>
#
# This script must be called from the root directory of a cobbzilla-wizard-style Java application.
# It will look in src/test/java for a DbInit class to call.
#
# The dbname should be the name of the test database that the DbInit class will be populating.
#

function die {
    echo 1>&2 "${1}"
    exit 1
}

DBNAME=${1}
if [ -x ${DBNAME} ] ; then
  die "Usage: $0 <dbname> <sql-file>"
fi

if [ -z "${DBADMIN}" ] ; then
  DBADMIN=""
else
  DBADMIN="-U ${DBADMIN}"
fi

SQLFILE="${2}"
if [ -z "${SQLFILE}" ] ; then
  die "Usage: $0 <dbname> <sql-file>"
fi
PARENT="$(dirname ${SQLFILE})"
if [ ! -d "${PARENT}" ] ; then
  mkdir ${PARENT} || die "sql-file parent directory does not exist and could not be created: ${PARENT}"
fi

# There should be exactly one class named "DbInit" in the test sources
DBINIT=$(find src/test/java -type f -name DbInit.java | sed -e 's,src/test/java/,,' | sed -e 's/.java$//' | tr '/' '.')
if [ -x ${DBINIT} ] ; then
  die "No DbInit class found in src/test/java"
fi

if [ -z "${GENSQL_NODROPCREATE}" ] ; then
  dropdb ${DBADMIN} ${DBNAME} || echo 1>&2 "Error dropping ${DBNAME} database"
  createdb ${DBADMIN} ${DBNAME} || die "Error creating ${DBNAME} database"
fi

MVN_LOG=$(mktemp /tmp/gen-sql.mvn.dbinit.XXXXXXX)
mvn -Dtest=${DBINIT} test 2>&1 > ${MVN_LOG} || die "Error populating ${DBNAME} database. ${MVN_LOG} has more info: $(cat ${MVN_LOG})"
rm -f ${MVN_LOG}

pg_dump ${DBADMIN} ${DBNAME} | sed -e "s/$(whoami)/postgres/g" > ${SQLFILE}
echo 1>&2 "Dumped SQL to ${SQLFILE}"