class Chef::Recipe::Git

  KNOWN_HOSTS = ".ssh/known_hosts"

  def self.add_github_hostkey (chef, run_as)
    github_hostkey = chef.node[:git][:github_hostkey]
    known_hosts = "~#{run_as}/#{KNOWN_HOSTS}"
    chef.bash "set github known host key for #{run_as}" do
      user 'root'
      code <<-EOF
sudo -u #{run_as} -H bash -c "echo '#{github_hostkey}' >> #{known_hosts}"
sudo -u #{run_as} -H chmod 644 #{known_hosts}
      EOF
      not_if { File.exists?(known_hosts) && `cat #{known_hosts}`.lines.grep(github_hostkey).size > 0 }
    end
  end

  def self.clone (chef, repo, branch, run_as, cwd, dir = nil)
    dir ||= base_name(repo)
    chef.bash "clone repository #{repo} on branch #{branch} into dir #{dir}" do
      user 'root'
      cwd cwd
      code <<-EOF
sudo -u #{run_as} -H git clone #{repo} --branch #{branch} #{dir}
      EOF
      not_if { File.exists? "#{cwd}/#{dir}"  }
    end
  end

  def self.base_name(repo)
    /\/([-\w]+)\.git/.match(repo)[1]
  end

end
