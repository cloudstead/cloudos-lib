#!/bin/bash
#
# Usage:
#   bash install.sh [run-list.json|cookbook-name]
#
# If neither run-list.json nor cookbook-name are provided, the default behavior is to install using solo.json as the run list
#
# If a run-list.json is provided, it will be used.
# If a cookbook-name is provided, only that cookbook will be installed (it need not be present in the default solo.json)
#

# This runs as root on the server
CHEF_PACKAGE="chef_11.10.4-1.ubuntu.12.04_amd64.deb"
CHEF_PACKAGE_URL="https://opscode-omnibus-packages.s3.amazonaws.com/ubuntu/12.04/x86_64/${CHEF_PACKAGE}"
THISDIR=$(pwd)
JSON=${THISDIR}/JSON.sh

chef_binary=/usr/bin/chef-solo

function die {
  echo 1>&2 "${1}"
  exit 1
}

function value_from_databag {
  databag=$1
  field=$2
  value=$(cat ${databag} | ${JSON} | grep '\["'${field}'"\]' | head -n 1 | tr -d '"[]' | awk '{print $2}' | tr -d ' ')
  if [ -z "${value}" ] ; then
    die "Field ${field} not found in databag ${databag}"
  fi
  echo "${value}"
}

RUN_LIST="${1}"
if [ -z "${RUN_LIST}" ] ; then
  RUN_LIST=solo.json

elif [ ! -f "${RUN_LIST}" ] ; then
  SINGLE_COOKBOOK="${RUN_LIST}"
  if [ ! -f ${THISDIR}/cookbooks/${SINGLE_COOKBOOK}/recipes/default.rb ] ; then
    die "Not a valid run-list or cookbook: ${SINGLE_COOKBOOK}"
  fi
  RUN_LIST=solo.json
fi

if [ -z "${SINGLE_COOKBOOK}" ] ; then
  SINGLE_COOKBOOK="${2}"
fi

# If a databag exists in base.json, and it includes a hostname and parent domain,
# then set the hostname before starting the chef run
NUM_BASE_DATABAGS=$(find data_bags -type f -name base.json | wc -l | tr -d ' ')
BASE_INIT_DATABAG="$(find data_bags -type f -name base.json)"
if [ ${NUM_BASE_DATABAGS} -gt 1 ] ; then
  die "More than one base.json databag found: ${BASE_INIT_DATABAG}"
fi
if [ ${NUM_BASE_DATABAGS} -eq 1 ] ; then
  TARGET_HOSTNAME=$(value_from_databag ${BASE_INIT_DATABAG} "hostname")
  TARGET_DOMAIN=$(value_from_databag ${BASE_INIT_DATABAG} "parent_domain")
  # Sanity check
  if [[ -z ${TARGET_HOSTNAME} || -z ${TARGET_DOMAIN} ]] ; then
    die "Databag ${BASE_INIT_DATABAG} was missing 'hostname' and/or 'parent_domain'"
  fi

  NEW_HOSTNAME="${TARGET_HOSTNAME}.${TARGET_DOMAIN}"
  if [ $(hostname) != "${NEW_HOSTNAME}" ] ; then
    hostname "${NEW_HOSTNAME}"
    echo "${NEW_HOSTNAME}" > /etc/hostname
  fi
  if [ $(cat /etc/hosts | grep ${NEW_HOSTNAME} | wc -l | tr -d ' ') -eq 0 ] ; then
    # needed for first-time run, so sudo commands do not print errors.
    # the chef run will overwrite /etc/hosts when the app with the base.json databag is installed
    IP=$(ifconfig | grep -A 3 eth0 | grep "inet addr:" | tr ':' ' ' | awk '{print $3}')
    if [ -z "${IP}" ] ; then
      die "Error determining IP address"
    fi
    echo "${IP} ${NEW_HOSTNAME}" >> /etc/hosts
  fi
fi

# Are we on a vanilla system?
if ! test -f "${chef_binary}"; then

    export DEBIAN_FRONTEND=noninteractive
    # Upgrade headlessly (this is only safe-ish on vanilla systems)
    aptitude update && apt-get -o Dpkg::Options::="--force-confnew" --force-yes -fuy dist-upgrade &&

    # Install Chef
    cd /tmp && curl -O ${CHEF_PACKAGE_URL} &&
    if [ $(dpkg -l | grep chef | grep -v chef-server | wc -l) -eq 0 ] ; then sudo dpkg -i ${CHEF_PACKAGE} ;  fi
fi &&

# If we are only being asked to install a single cookbook, create a custom run-list file just for that
if [ ! -z ${SINGLE_COOKBOOK} ] ; then
  SC_RUN_LIST="${THISDIR}/solo-${SINGLE_COOKBOOK}.json"

  # header
  echo "{ \"run_list\": [ " > ${SC_RUN_LIST}

  # add all lib recipes
  for lib in $(find $(find cookbooks -type d -name recipes) -type f -name lib.rb | xargs dirname | xargs dirname | xargs -n 1 basename) ; do
    echo -n "\"recipe[${lib}::lib]\", " >> ${SC_RUN_LIST}
  done

  # add the single default recipe
  echo -n "\"recipe[${SINGLE_COOKBOOK}]\"" >> ${SC_RUN_LIST}

  # add validate recipe if it exists
  if [ -f ${THISDIR}/cookbooks/${SINGLE_COOKBOOK}/recipes/validate.rb ] ; then
    echo -n ", \"recipe[${SINGLE_COOKBOOK}::validate]\" " >> ${SC_RUN_LIST}
  fi

  # footer
  echo "] }" >> ${SC_RUN_LIST}

  RUN_LIST="${SC_RUN_LIST}"
fi

cd ${THISDIR} && "${chef_binary}" -c solo.rb -j ${RUN_LIST} -l debug

if [[ $(service apache2 status) =~ " not running" ]] ; then
  service apache2 restart
fi