#!/bin/bash
#
# Usage:
#   bash uninstall.sh cookbook-name
#
# Un-installs the named cookbook from the solo.json run list.
#
# If the cookbook has a cookbook::uninstall recipe, that recipe is run.
# Then the cookbook is removed from the solo.json run list and from the local chef "cookbooks" and "databags" directories.
#
# todo: if the env var CLOUDOS_BACKUP_APP is set, the app will be backed up before anything else is done
#

THISDIR=$(pwd)
JSON=${THISDIR}/JSON.sh

function die {
    echo 1>&2 "${1}"
    exit 1
}

chef_binary=/usr/bin/chef-solo
chef_user=$(cat /etc/chef-user)

if [ -z "${chef_user}" ] ; then
  die "No chef user found in /etc/chef-user"
fi

COOKBOOK="${1:?No cookbook arg provided}"

SOLO_JSON=${THISDIR}/solo.json
if [ ! -f "${SOLO_JSON}" ] ; then
  die "${SOLO_JSON} not found"
fi

if [ ! -z "${CLOUDOS_BACKUP_APP}" ] ; then
    echo "todo: backup app before continuing"
fi

# If this is not an 'app', do not allow uninstallation via this script.
MANIFEST="${THISDIR}/data_bags/${COOKBOOK}/cloudos-manifest.json"
if [ ! -f "${MANIFEST}" ] ; then
    die "${COOKBOOK}: No manifest found in ${MANIFEST}"
fi
LEVEL=$(cat "${MANIFEST}" | ./JSON.sh -l | grep '\[\"level\"\]' | tr -d '"' | awk '{print $2}')
if [ -z "${LEVEL}" ] ; then
    die "${COOKBOOK} has no 'level' defined in manifest ${MANIFEST}, refusing to uninstall"
fi
if [ "${LEVEL}" != "app" ] ; then
    die "${COOKBOOK} level is ${LEVEL}, refusing to uninstall"
fi

# Run uninstall recipe if we have one
if [ -f "${THISDIR}/cookbooks/${COOKBOOK}/recipes/uninstall.rb" ] ; then

    UNINSTALL_SOLO="${THISDIR}/solo-${COOKBOOK}-uninstall.json"

    # header
    echo "{ \"run_list\": [ " > ${UNINSTALL_SOLO}

    # add all lib recipes
    for lib in $(cat ${SOLO_JSON} | egrep -v '^[[:space:]]*//' | ${JSON} | grep "\"run_list\"," | grep ::lib | awk '{print $2}') ; do
        echo -n "${lib}, " >> ${UNINSTALL_SOLO}
    done

    # add the uninstall recipe
    echo -n "\"recipe[${COOKBOOK}::uninstall]\"" >> ${UNINSTALL_SOLO}

    # footer
    echo "] }" >> ${UNINSTALL_SOLO}

    chown ${chef_user} ${UNINSTALL_SOLO} || die "Error chaninging ownership of ${UNINSTALL_SOLO}"
    cd ${THISDIR} && "${chef_binary}" -c solo.rb -j ${UNINSTALL_SOLO} -l debug || die "Error running chef"

elif [ ! -d "${THISDIR}/cookbooks/${COOKBOOK}" ] ; then
    echo "WARNING: ${COOKBOOK} is not installed."
fi

# Remove from default run list, removing trailing comma if there is one
TMP=$(mktemp /tmp/uninstall_sh_XXXXXXX.json) || die "Error creating temp file"
cat ${SOLO_JSON} | sed -e 's/,/,\
/g' | grep -v ${COOKBOOK} | tr -d '\n ' | sed -e 's/,]}/]}/' > ${TMP}

if [ ! -s ${TMP} ] ; then
  die "solo.json couldn't be updated"
fi

mv ${TMP} ${SOLO_JSON} || die "Error moving ${TMP} -> ${SOLO_JSON}"
chown ${chef_user} ${SOLO_JSON} || die "Error chaning ownership of ${SOLO_JSON}"

rm -rf ${THISDIR}/cookbooks/${COOKBOOK}
rm -rf ${THISDIR}/data_bags/${COOKBOOK}
rm -rf ${THISDIR}/data_files/${COOKBOOK}
