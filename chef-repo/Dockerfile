#
# Cloudstead Base Docker Image
#
# Base on phusion/baseimage, this does the minimal work to prepare the container for its first chef run
#
# NOTE: Do create an image based on this Dockerfile from its original location.
#       This Dockerfile is designed to work in conjunction with the deploy_lib.sh script.
#       Copy the Dockerfile to your chef-repo directory and run your deploy script, instead
#       of using a target of user@host, you can use a target of docker:image_id
#

FROM phusion/baseimage:0.9.17

MAINTAINER Jonathan Cobb <jonathan@cloudstead.io>

# Use baseimage-docker's init system.
CMD ["/sbin/my_init"]

# Install our SSH key
COPY docker_key.pub /tmp/docker_key.pub
RUN cat /tmp/docker_key.pub >> /root/.ssh/authorized_keys && rm -f /tmp/docker_key.pub

# Update the system
RUN apt-get update

# Clean up APT when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Enable SSH
RUN rm -f /etc/service/sshd/down

# Add ubuntu user with chef powers
RUN useradd -m ubuntu && \
  echo "" >> /etc/sudoers && \
  echo "ubuntu  ALL = (ALL) NOPASSWD:ALL" >> /etc/sudoers

# Create "chef-user" file, accessible only to root
RUN echo "ubuntu" > /etc/chef-user && chown root.root /etc/chef-user && chmod 600 /etc/chef-user

# Copy chef-repo, set perms
COPY . /home/ubuntu/chef/
RUN chown -R ubuntu /home/ubuntu && \
  for dir in data_bags data_files certs ; do \
    if [ -d ${dir} ] ; then chmod -R 700 ${dir} || exit 1 ; fi \
  done

# Run chef
RUN cd /home/ubuntu/chef && \
 start=$(date) && \
 sudo bash install.sh 2>&1 | tee chef.out && \
 echo "chef-run started at ${start}" | tee -a chef.out && \
 echo "chef-run ended   at $(date)"  | tee -a chef.out || exit 1 ; sudo rm -rf /tmp/*

