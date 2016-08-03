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
chef_gem=/opt/chef/embedded/bin/gem

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
BASE_DATABAG="$(find data_bags -type f -name base.json)"
if [ ${NUM_BASE_DATABAGS} -gt 1 ] ; then
  die "More than one base.json databag found: ${BASE_DATABAG}"
fi
if [ ${NUM_BASE_DATABAGS} -eq 1 ] ; then
  TARGET_HOSTNAME=$(value_from_databag ${BASE_DATABAG} "hostname")
  TARGET_DOMAIN=$(value_from_databag ${BASE_DATABAG} "parent_domain")
  # Sanity check
  if [[ -z ${TARGET_HOSTNAME} || -z ${TARGET_DOMAIN} ]] ; then
    die "Databag ${BASE_DATABAG} was missing 'hostname' and/or 'parent_domain'"
  fi

  if [[ ${TARGET_HOSTNAME} != "localhost" ]] ; then
    NEW_HOSTNAME="${TARGET_HOSTNAME}.${TARGET_DOMAIN}"
    if [ $(hostname) != "${NEW_HOSTNAME}" ] ; then
      hostname "${NEW_HOSTNAME}"
      echo "${NEW_HOSTNAME}" > /etc/hostname
    fi
    IP=$(cat ${BASE_DATABAG} | ${JSON} | grep '\["'public_ip'"\]' | head -n 1 | tr -d '"[]' | awk '{print $2}' | tr -d ' ')
    if [ -z "${IP}" ] ; then
      IP=$(ifconfig | grep -A 3 eth0 | grep "inet addr:" | tr ':' ' ' | awk '{print $3}')
      if [ -z "${IP}" ] ; then
        die "Error determining IP address"
      fi
    fi
    if [ $(cat /etc/hosts | egrep "^${IP}[[:space:]]+${NEW_HOSTNAME}" | wc -l | tr -d ' ') -eq 0 ] ; then
      # needed for first-time run, so sudo commands do not print errors.
      # the chef run will overwrite /etc/hosts with the info from the base.json databag
      echo "" >> /etc/hosts
      echo "# Added by chef install script: $0" >> /etc/hosts
      echo "${IP} ${NEW_HOSTNAME}" >> /etc/hosts
    fi
  fi
fi

export DEBIAN_FRONTEND=noninteractive

# Are we on a vanilla system?
if ! test -f "${chef_binary}"; then

    # Upgrade headlessly (this is only safe-ish on vanilla systems)
    apt-get update && apt-get -o Dpkg::Options::="--force-confnew" --force-yes -fuy dist-upgrade &&

    # Install Chef
    if [ $(dpkg -l | grep chef | grep -v chef-server | wc -l) -eq 0 ] ; then
      cd /tmp && curl -O ${CHEF_PACKAGE_URL} && sudo dpkg -i ${CHEF_PACKAGE} || die "Error installing Chef package ${CHEF_PACKAGE}"
    fi

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

# Some apps may require us to install gems for chef in order to work
# For example, the kolab app requires the inifile gem to parse the kolab.conf file
apps=$(cd ${THISDIR} && cat ${RUN_LIST} | sed -e 's,//.*,,' | ${THISDIR}/JSON.sh | grep \"run_list\", | tr '[]' '  ' | awk '{print $3}' | sed -e 's/::.*//')
for app in ${apps} ; do
  gems_file="${THISDIR}/cookbooks/${app}/files/default/installer_gems"
  if [ -f "${gems_file}" ] ; then
    for gem in $(cat ${gems_file}) ; do
      if [ $(${chef_gem} list | egrep -- "${gem} \(" | wc -l | tr -d ' ') -eq 0 ] ; then
        ${chef_gem} install ${gem} || die "Error installing chef-gem ${gem}"
      fi
    done
  fi
done

if [ -x ${THISDIR}/pre_install.sh ] ; then
  ${THISDIR}/pre_install.sh || die "Error running pre_install.sh: exit status $?"
fi

cd ${THISDIR} && "${chef_binary}" -c solo.rb -j ${RUN_LIST} -l debug

if [[ $(service apache2 status) =~ " not running" ]] ; then
  service apache2 restart
fi