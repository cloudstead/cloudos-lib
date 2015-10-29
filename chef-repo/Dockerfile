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

# Update the system
RUN apt-get update

# Clean up APT when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Enable SSH
RUN rm -f /etc/service/sshd/down

# Add ubuntu user with chef powers
RUN useradd --home /home/ubuntu --create-home --shell /bin/bash --password '_login_with_password_disabled_' ubuntu && \
  echo "" >> /etc/sudoers && \
  echo "ubuntu  ALL = (ALL) NOPASSWD:ALL" >> /etc/sudoers

# Install ubuntu SSH key
COPY docker_key.pub /tmp/docker_key.pub
RUN mkdir /home/ubuntu/.ssh && \
  chmod 700 /home/ubuntu/.ssh && \
  cat /tmp/docker_key.pub >> /home/ubuntu/.ssh/authorized_keys && \
  chmod 600 /home/ubuntu/.ssh/authorized_keys && \
  chown -R ubuntu.ubuntu /home/ubuntu/.ssh && \
  rm -f /tmp/docker_key.pub

# Create docker marker file, indicates we are in a docker container
RUN echo "true" > /etc/is-docker && chown root.root /etc/is-docker && chmod 644 /etc/is-docker

# Copy chef-repo, set perms
COPY . /home/ubuntu/chef/
RUN for dir in data_bags data_files certs ; do \
      if [ -d ${dir} ] ; then chmod -R 700 ${dir} || exit 1 ; fi \
    done
RUN chown -R ubuntu /home/ubuntu/chef

# Ready for chef run
