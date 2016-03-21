#!/bin/bash
#
# Usage: deploy_lib.sh [target] [init-files] [required] [cookbook-sources] [solo-json-file] [tempdir|inline]
#
# Run this from the chef-repo directory containing the solo.json that will drive the chef run
#
# Arguments:
#   target:           where to deploy, can be:
#                        user@host            -- for ssh deploys
#                        docker:tag           -- create a new docker container
#                        docker:container-id  -- re-deploy to an existing container
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

function value_from_databag {
  local databag=$1
  local field=$2
  local search=$(echo "\[\"$(echo "${field}" | sed -e 's_\._","_g')\"\]")
  local value=$(cat ${databag} | ${JSON} | grep "${search}" | head -n 1 | tr -d '"[]' | awk '{print $2}' | tr -d ' ')
  if [ -z "${value}" ] ; then
    die "Field ${field} not found in databag ${databag}"
  fi
  echo "${value}"
}

DEPLOY_RESULT=""

function deploy_ssh {
  local chef="${1}"
  local ssh_opts="${2}"
  local deploy_target="${3}"

  cd ${chef} && \
  ssh -o 'StrictHostKeyChecking no' ${ssh_opts} "${deploy_target}" 'rm -rf ~/chef && mkdir -p ~/chef' && \
  rsync -avzc -e "ssh -o 'StrictHostKeyChecking no' ${ssh_opts}" ./* ${deploy_target}:chef/ && \
  ssh -o 'StrictHostKeyChecking no' ${ssh_opts} "${deploy_target}" '
  start=$(date) &&
  t=$(mktemp /tmp/chef-user.XXXXXXX) &&
    echo $(whoami) > ${t} &&
    sudo cp ${t} /etc/chef-user &&
    rm -f ${t} &&
    cd ~/chef &&
    for dir in data_bags data_files certs ; do if [ -d ${dir} ] ; then chmod -R 700 ${dir} || exit 1 ; fi ; done &&
    sudo bash install.sh 2>&1 | tee chef.out &&
    echo "chef-run started at ${start}" | tee -a chef.out &&
    echo "chef-run ended   at $(date)"  | tee -a chef.out ;
    sudo rm -rf /tmp/*'
  local rval=$?

  cd ${BASE}
  if [ "${MODE}" = "tempdir" ] ; then
    rm -rf ${TEMP}
  # if you want to keep chef run dirs, comment out the line above, and uncomment the line below
  # echo "chef run is in ${CHEF}"
  else
    echo "chef run is in ${CHEF}"
  fi
  DEPLOY_RESULT=${rval}
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
  deploy_ssh "${CHEF}" "${SSH_OPTS}" "${DEPLOY_TARGET}"
  if [ ${DEPLOY_RESULT} -ne 0 ] ; then
    die "Error running chef: exit code ${DEPLOY_RESULT}"
  fi

else
  # Deploy target is docker:tag or docker:container_id
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

  # Construct docker hostname
  BASE_DATABAG="${CHEF}/data_bags/base/base.json"
  base_host=$(value_from_databag ${BASE_DATABAG} "hostname")
  base_domain=$(value_from_databag ${BASE_DATABAG} "parent_domain")
  if [[ -z "${base_host}" || -z "${base_domain}" ]] ; then
    die "Base databag was missing hostname and/or parent_domain: ${BASE_DATABAG}"
  fi
  DOCKER_HOSTNAME="${base_host}.${base_domain}"

  # Check if deploy target is docker:container_id
  if [ $(docker ps -q | awk '{print $1}' | grep ${DOCKER_TAG} | wc -l) -eq 1 ] ; then
    IP="$(docker inspect -f "{{ .NetworkSettings.IPAddress }}" ${DOCKER_TAG})"
    if [ -z "${IP}" ] ; then
      die "No IP address found for existing docker container ${DOCKER_TAG}"
    fi
    DEPLOY_TARGET="ubuntu@${IP}"
    deploy_ssh "${CHEF}" "${SSH_OPTS}" "${DEPLOY_TARGET}"
    if [ ${DEPLOY_RESULT} -ne 0 ] ; then
      die "Error running chef: exit code ${DEPLOY_RESULT}"
    fi

  else
    docker_build_log=$(mktemp /tmp/docker_build.XXXXXXX.log)
    docker_container_id_file=$(mktemp /tmp/docker_run.XXXXXXX.log)

    # Any ports to expose?
    expose=""
    if [ ! -z "${DOCKER_EXPOSE_PORTS}" ] ; then
      for port in ${DOCKER_EXPOSE_PORTS} ; do
        expose="${expose} -p ${port}"
      done
    fi

    cd ${CHEF} && \
      cp ${SSH_KEY}.pub ./docker_key.pub && \
      echo "Building docker image from $(pwd) with tag ${DOCKER_TAG}, logging to ${docker_build_log}" && \
      image_id=$(docker build -t ${DOCKER_TAG} . | tee ${docker_build_log} | grep "Successfully built" | awk '{print $3}') && \
      if [ -z "${image_id}" ] ; then
        die "Error building docker image (pwd=$(pwd)): $(cat ${docker_build_log})"
      fi && \
      echo "Starting docker container from image ${image_id} with tag ${DOCKER_TAG}" && \
      docker run ${expose} \
        --hostname ${DOCKER_HOSTNAME} \
        --name ${DOCKER_TAG#*:} \
        --detach \
        ${image_id} > ${docker_container_id_file}
    rval=$?
    docker_container_id="$(cat ${docker_container_id_file})"
    if [[ ${rval} -ne 0 || -z "${docker_container_id}" ]] ; then
      if [ ! -z "${docker_container_id}" ] ; then docker rm -f ${docker_container_id} ; fi
      docker rmi ${image_id}
      die "Error starting docker container"
    fi

    IP="$(docker inspect -f "{{ .NetworkSettings.IPAddress }}" ${docker_container_id})"
    if [ -z "${IP}" ] ; then
      docker rm -f ${docker_container_id}
      docker rmi ${image_id}
      die "No IP address found for newly-created docker container ${docker_container_id}"
    fi

    # Wait for ssh to come up
    start=$(date +%s)
    while ! nc -z ${IP} 22 ; do
      if [ $(expr $(date +%s) - ${start}) -gt 30 ] ; then
        docker rm -f ${docker_container_id}
        docker rmi ${image_id}
        die "Timed out waiting for sshd to come up on container ${docker_container_id}"
      fi
      sleep 1s
    done

    # Run chef
    echo "Docker container '${DOCKER_TAG}' successfully started: ${docker_container_id}"
    DEPLOY_TARGET="ubuntu@${IP}"

    # Create docker control script
    if [ -z "${DOCKER_SSH_DIR}" ] ; then
      DOCKER_SSH_DIR="/usr/local/bin/docker"
    fi
    mkdir -p ${DOCKER_SSH_DIR}
    CONTROL="${DOCKER_SSH_DIR}/${DOCKER_TAG#*:}.sh"
    ctime=$(date +%s)
    cat > ${CONTROL} <<EOF
#!/bin/bash
legal_commands="ssh ip ports cid ctime json destroy"
op=\${1:?no operation, use: \${legal_commands}}
case \${op} in
  ssh)
    ssh -i ${SSH_KEY} ${DEPLOY_TARGET}
    exit \$?
    ;;
  ip)
    echo "${IP}" && exit 0
    ;;
  ports)
    echo "\$(docker port ${docker_container_id})" && exit 0
    ;;
  cid)
    echo "${docker_container_id}" && exit 0
    ;;
  ctime)
    echo "${ctime}" && exit 0
    ;;
  json)
    echo "{
    \"ctime\": \"${ctime}\",
    \"hostname\": \"${DOCKER_HOSTNAME}\",
    \"ports\": \"\$(docker port ${docker_container_id})\",
    \"chef_dir\": \"${CHEF}\",
    \"image_id\": \"${image_id}\",
    \"container_id\": \"${docker_container_id}\",
    \"tag\": \"${DOCKER_TAG}\",
    \"ssh_key\": \"${SSH_KEY}\",
    \"ip\": \"${IP}\",
    \"ssh_cmd\": \"ssh -i ${SSH_KEY} ${DEPLOY_TARGET}\",
}"
    exit 0
    ;;
  destroy)
    docker rm -f ${docker_container_id}
    docker rmi ${image_id}
    rm -rf ${CHEF} ${CONTROL}
    exit 0
    ;;
  *)
    echo "Unsupported operation \${op}, use: \${legal_commands}"
    exit 1
    ;;
esac
EOF
    chown $(whoami) ${DOCKER_SSH_DIR}/${DOCKER_TAG#*:}.sh
    chmod 755 ${DOCKER_SSH_DIR}/${DOCKER_TAG#*:}.sh

    deploy_ssh "${CHEF}" "${SSH_OPTS}" "${DEPLOY_TARGET}"
    if [ ${DEPLOY_RESULT} -ne 0 ] ; then
#      docker rm -f ${docker_container_id}
#      docker rmi ${image_id}
      die "Error running chef: exit code ${DEPLOY_RESULT}"
    fi

    rm -f ${docker_build_log} ${docker_container_id_file}
  fi
fi
