#!/bin/bash
#
# Usage: deploy.sh [target] [init-files] [required] [cookbook-sources] [solo-json-file]
#
# Run this from the chef-repo directory containing the solo.json that will drive the chef run
#
# Arguments:
#   target:           the user@host to deploy to
#   init-files:       directory containing init files (dirs should be data_bags, data_files and certs)
#   required:         list of required files in the init-files dir (use quotes). paths are relative to init-files dir.
#   cookbook-sources: list of directories that contain cookbooks (use quotes)
#   solo-json-file:   run list to use for chef solo run
#

function die {
  echo 1>&2 "${1}"
  exit 1
}

if [ ! -f $(pwd)/solo.json ] ; then
  die "ERROR: Current directory does not contain solo.json, required for deployment: $(pwd)"
fi

host="${1:?no user@host specified}"
INIT_FILES="${2:?no init-files dir specified}"
REQUIRED="${3:?no required specified}"
COOKBOOK_SOURCES="${4:?no cookbook sources specified}"
SOLO_JSON="${5:-./solo.json}"

# The host key might change when we instantiate a new VM, so
# we remove (-R) the old host key from known_hosts
ssh-keygen -R "${host#*@}" 2> /dev/null

if [ -z "${INIT_FILES}" ] ; then
  die "ERROR: INIT_FILES is not defined in the environment."
fi
INIT_FILES=$(cd ${INIT_FILES} && pwd) # make into absolute path

# Required init files
for required in $(echo ${REQUIRED} | tr '\n' ' ') ; do
  if [ ! -f ${INIT_FILES}/${required} ] ; then
    die "ERROR: file was missing: ${INIT_FILES}/${required}"
  fi
done

LIB_BASE=$(cd $(dirname $0) && pwd)
BASE=$(pwd)
COOKBOOK_SOURCES=$(echo ${BASE}/cookbooks ${COOKBOOK_SOURCES} | tr '\n' ' ')

# Extract cookbooks from solo.json run list
COOKBOOKS="$(cat ${BASE}/solo.json | egrep -v '[[:blank:]]*//' | ${LIB_BASE}/JSON.sh  | grep '\["run_list",' | awk '{print $2}' | sed 's/recipe//' | tr -d '"[]' | tr ':' ' ' | awk '{print $1}' | sort | uniq)"
if [ -z "$(echo ${COOKBOOKS} | tr -d '[:blank:]\n\r')" ]  ; then
  die "ERROR: no cookbooks could be gleaned from run list: ${BASE}/solo.json"
fi

# create staging area with our cookbooks, shared cookbooks, and scripts
mkdir -p ${BASE}/chef-runs
TEMP=$(mktemp -d ${BASE}/chef-runs/run.$(date +%Y_%m_%d).XXXXXX)

# cookbooks...
mkdir -p ${TEMP}/cookbooks/
mkdir -p ${TEMP}/data_bags/
for cookbook in ${COOKBOOKS} ; do
  found=0
  for source in ${COOKBOOK_SOURCES} ; do
    if [ ! -d ${source} ] ; then
      echo 1>&2 "WARNING: cookbook source not found: ${source}"
      continue
    fi
    if [ -d ${source}/${cookbook} ] ; then
      # Copy cookbook files
      rsync -vac ${source}/${cookbook} ${TEMP}/cookbooks/

      # If there is a manifest in the databags dir, copy that too
      manifest="${source}/../data_bags/${cookbook}/cloudos-manifest.json"
      if [ -f ${manifest} ] ; then
        mkdir -p ${TEMP}/data_bags/${cookbook} && \
        rsync -vac ${manifest} ${TEMP}/data_bags/${cookbook}/
      fi
      found=1
      break
    fi
  done

  if [ ${found} -eq 0 ] ; then
    die "ERROR: cookbook ${cookbook} not found in any sources: ${COOKBOOK_SOURCES}"
  fi
done

# bootstrap files and run list...
for f in JSON.sh install.sh uninstall.sh solo.rb ; do
  cp ${LIB_BASE}/${f} ${TEMP}/
done
for f in ${BASE}/solo*.json  ; do
  cp ${f} ${TEMP}/
done
cp ${SOLO_JSON} ${TEMP}/solo.json || die "ERROR: ${SOLO_JSON} could not be copied to ${TEMP}/solo.json"

# data_bags, data and certs...
rsync -vac ${INIT_FILES}/* ${TEMP}/
for dir in data_bags data certs ; do
  if [ -d ${TEMP}/${dir} ] ; then
    chmod -R 700 ${TEMP}/${dir}
  fi
done

if [ -z ${SSH_KEY} ] ; then
  SSH_OPTS=""
else
  SSH_OPTS="-i ${SSH_KEY}"
  cp ${SSH_KEY} /tmp/ckey
fi

#echo "chef run is in ${TEMP}, cookbooks were ${COOKBOOKS}"
#exit 1

# Let's roll. Time to:
# - copy the tarball over,
# - write our username to /etc/chef-user
# - delete ~/chef dir
# - unpack fresh chef-repo
# - run chef-solo
cd ${TEMP} &&  tar cj . | ssh ${SSH_OPTS} -o 'StrictHostKeyChecking no' "$host" '
start=$(date) &&
t=$(mktemp /tmp/chef-user.XXXXXXX) &&
  echo $(whoami) > ${t} &&
  sudo cp ${t} /etc/chef-user &&
  rm -f ${t} &&
  sudo rm -rf ~/chef &&
  mkdir ~/chef &&
  cd ~/chef &&
  tar xj &&
  chmod -R 700 data_bags data_files certs &&
  sudo bash install.sh 2>&1 | tee chef.out &&
  echo "chef-run started at ${start}" | tee -a chef.out &&
  echo "chef-run ended   at $(date)"  | tee -a chef.out ;
  sudo rm -rf /tmp/*'
rval=$?

cd ${BASE}
rm -rf ${TEMP}
# if you want to keep chef run dirs, comment out the line above, and uncomment the line below
# echo "chef run is in ${TEMP}"

if [ ${rval} -ne 0 ] ; then
    die "Error running chef: exit code ${rval}"
fi
