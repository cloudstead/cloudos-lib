#!/bin/bash
#
# Usage: deploy_lib.sh [target] [init-files] [required] [cookbook-sources] [solo-json-file] [tempdir|inline]
#
# Run this from the chef-repo directory containing the solo.json that will drive the chef run
#
# Arguments:
#   target:           where to deploy, can be user@host or docker:tag
#   init-files:       directory containing init files (dirs should be data_bags, data_files and certs)
#   required:         space-separated list of required files in the init-files dir (use quotes). paths are relative to init-files dir.
#   cookbook-sources: space-separated list of directories that contain cookbooks (use quotes)
#   solo-json-file:   run list to use for chef solo run
#   mode:             default is 'tempdir' which will create a new temp dir with init files added. 'inline' will assume the current directory is the chef repo, and cookbook-sources is ignored.
#

function die {
  echo 1>&2 "${1}"
  exit 1
}

LIB_BASE=$(cd $(dirname $0) && pwd)
BASE=$(pwd)
DEPLOY_TARGET="${1:?no deploy target specified. Use user@host or docker:tag}"
INIT_FILES="${2:?no init-files dir specified}"
REQUIRED="${3:?no required specified}"
COOKBOOK_SOURCES="${4:?no cookbook sources specified}"
SOLO_JSON="${5:-${BASE}/solo.json}"
MODE="${6:-tempdir}"

if [ ! -f ${SOLO_JSON} ] ; then
  die "ERROR: ${SOLO_JSON} does not exist, required for deployment"
fi

DOCKER_TAG=""
if [[ "${DEPLOY_TARGET}" =~ "docker:" ]] ; then
  DOCKER_TAG="${DEPLOY_TARGET#*:}"
else
  # The host key might change when we instantiate a new VM, so
  # we remove (-R) the old host key from known_hosts
  ssh-keygen -R "${DEPLOY_TARGET#*@}" 2> /dev/null
fi

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

JSON="${LIB_BASE}/JSON.sh"
if [ ! -x ${JSON} ] ; then chmod u+x ${JSON} ; fi

# create staging area with our cookbooks, shared cookbooks, and scripts
mkdir -p ${BASE}/chef-runs
if [ "${MODE}" = "tempdir" ] ; then
  TEMP=$(mktemp -d ${BASE}/chef-runs/run.$(date +%Y_%m_%d).XXXXXX)
  CHEF="${TEMP}"

  # bootstrap files and run list...
  for f in JSON.sh install.sh uninstall.sh solo.rb Dockerfile ; do
    cp ${LIB_BASE}/${f} ${CHEF}/ || die "ERROR: ${LIB_BASE}/${f} could not be copied to ${CHEF}"
  done
  for f in ${BASE}/solo*.json  ; do
    cp ${f} ${CHEF}/ || die "ERROR: ${f} could not be copied to ${CHEF}"
  done
  cp ${SOLO_JSON} ${CHEF}/solo.json || die "ERROR: ${SOLO_JSON} could not be copied to ${CHEF}/solo.json"

  # Extract cookbooks from solo.json run list
  COOKBOOK_SOURCES=$(echo ${BASE}/cookbooks ${COOKBOOK_SOURCES} | tr '\n' ' ')
  COOKBOOKS="$(cat ${SOLO_JSON} | sed -e 's,//.*,,' | ${JSON} | grep '\["run_list",' | awk '{print $2}' | sed 's/recipe//' | tr -d '"[]' | tr ':' ' ' | awk '{print $1}' | sort | uniq)"
  if [ -z "$(echo ${COOKBOOKS} | tr -d '[:blank:]\n\r')" ]  ; then
    die "ERROR: no cookbooks could be gleaned from run list: ${SOLO_JSON}"
  fi

  # cookbooks...
  mkdir -p ${CHEF}/cookbooks/
  mkdir -p ${CHEF}/data_bags/
  for cookbook in ${COOKBOOKS} ; do
    if [ -d ${CHEF}/cookbooks/${cookbook} ] ; then
      echo 1>&2 "INFO: using cookbook: ${cookbook}"
      continue
    fi
    found=0
    for source in ${COOKBOOK_SOURCES} ; do
      if [ ! -d ${source} ] ; then
        echo 1>&2 "WARNING: cookbook source not found: ${source}"
        continue
      fi
      if [ -d ${source}/${cookbook} ] ; then
        # Copy cookbook files
        rsync -vac ${source}/${cookbook} ${CHEF}/cookbooks/

        # Copy databags, but only if they don't already exist
        databags="${source}/${cookbook}/../../data_bags/${cookbook}"
        if [ -d ${databags} ] ; then
          mkdir -p ${CHEF}/data_bags/${cookbook}
          cp -Rn ${databags}/* ${CHEF}/data_bags/${cookbook}/
        fi
        # If there is a manifest in the databags dir, copy that too
        manifest="${source}/../data_bags/${cookbook}/cloudos-manifest.json"
        if [ -f ${manifest} ] ; then
          mkdir -p ${CHEF}/data_bags/${cookbook} && \
          rsync -vac ${manifest} ${CHEF}/data_bags/${cookbook}/
        fi
        found=1
        break
      fi
    done

    if [ ${found} -eq 0 ] ; then
      die "ERROR: cookbook ${cookbook} not found in any sources: ${COOKBOOK_SOURCES}"
    fi
  done

elif [ "${MODE}" = "inline" ] ; then
  CHEF="${BASE}"
  if ! cmp --silent ${SOLO_JSON} ${CHEF}/solo.json ; then
    cp ${SOLO_JSON} ${CHEF}/solo.json || die "ERROR: ${SOLO_JSON} could not be copied to ${CHEF}/solo.json"
  fi

else
  die "Unrecognized mode: ${MODE}"
fi

# data_bags, data and certs...
rsync -vac ${INIT_FILES}/* ${CHEF}/
for dir in data_bags data_files certs ; do
  if [ -d ${CHEF}/${dir} ] ; then
    chmod -R 700 ${CHEF}/${dir}
  fi
done

if [ -z ${SSH_KEY} ] ; then
  SSH_OPTS=""
else
  SSH_OPTS="-i ${SSH_KEY}"
fi

# Let's roll. Time to:
# - copy the tarball over,
# - write our username to /etc/chef-user
# - delete ~/chef dir
# - unpack fresh chef-repo
# - run chef-solo
if [ -z "${DOCKER_TAG}" ] ; then
  # Deploy target is user@host -- use SSH to deploy
  cd ${CHEF} && tar cj . | ssh ${SSH_OPTS} -o 'StrictHostKeyChecking no' "${DEPLOY_TARGET}" '
  start=$(date) &&
  t=$(mktemp /tmp/chef-user.XXXXXXX) &&
    echo $(whoami) > ${t} &&
    sudo cp ${t} /etc/chef-user &&
    rm -f ${t} &&
    sudo rm -rf ~/chef &&
    mkdir ~/chef &&
    cd ~/chef &&
    tar xj &&
    for dir in data_bags data_files certs ; do if [ -d ${dir} ] ; then chmod -R 700 ${dir} || exit 1 ; fi ; done &&
    sudo bash install.sh 2>&1 | tee chef.out &&
    echo "chef-run started at ${start}" | tee -a chef.out &&
    echo "chef-run ended   at $(date)"  | tee -a chef.out ;
    sudo rm -rf /tmp/*'
  rval=$?

  cd ${BASE}
  if [ "${MODE}" = "tempdir" ] ; then
    rm -rf ${TEMP}
  # if you want to keep chef run dirs, comment out the line above, and uncomment the line below
  # echo "chef run is in ${CHEF}"
  else
   echo "chef run is in ${CHEF}"
  fi

  if [ ${rval} -ne 0 ] ; then
      die "Error running chef: exit code ${rval}"
  fi

else
  # Deploy target is docker:tag
  if [ -z "${SSH_KEY}" ] ; then
    if [ -f "${HOME}/.ssh/id_dsa" ] ; then
      SSH_KEY="${HOME}/.ssh/id_dsa"
    elif [ -f "${HOME}/.ssh/id_rsa" ] ; then
      SSH_KEY="${HOME}/.ssh/id_rsa"
    else
      die "SSH_KEY not defined in environment, and ${HOME}/.ssh did not contain id_dsa nor id_rsa"
    fi
  fi
  if [ ! -f "${SSH_KEY}.pub" ] ; then
    die "SSH public key file does not exist: ${SSH_KEY}.pub"
  fi

  docker_build_log=$(mktemp /tmp/docker_build.XXXXXXX.log)
  docker_container_id=$(mktemp /tmp/docker_run.XXXXXXX.log)

  cd ${CHEF} && \
    cp ${SSH_KEY}.pub ./docker_key.pub && \
    image_id=$(docker build -t ${DOCKER_TAG} . | tee ${docker_build_log} | grep "Successfully built" | awk '{print $3}') && \
    if [ -z "${image_id}" ] ; then
      die "Error building docker image (pwd=$(pwd)): $(cat ${docker_build_log})"
    fi && \
    docker run -d ${image_id} > ${docker_container_id} && \
    if [ -z "$(cat ${docker_container_id})" ] ; then
      die "Error starting docker container"
    fi
    echo "Docker container '${DOCKER_TAG}' successfully launched: $(docker inspect -f "{{ .NetworkSettings.IPAddress }}" $(cat ${docker_container_id}))" \

#  rm -f ${docker_build_log} ${docker_container_id}
fi
