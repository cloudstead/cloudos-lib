cloudos-lib
===========

* src: a Java facade for cloud compute and storage resources.
* chef-repo: set of common cookbooks used by cloudos-server

# Compute

## Create a cloud

Here's an example using DigitalOcean:

    final CsCloudConfig config = new CsCloudConfig()
            .withAccountId(accountId
            .withAccountSecret(accountSecret)
            .withRegion("sfo1")
            .withInstanceSize("2gb")
            .withImage("ubuntu-14-04-x64")
            .withGroupPrefix("mygroup-")
            .withProvider(JcloudBase.PROVIDER_DIGITALOCEAN)
            .withUser("ubuntu")
            .withDomain("example.com")
            .withCloudClass(DigitalOceanCloud.class.getName());

    final CsCloud cloud = new CsCloudFactory().buildCloud(config);

## Start an instance

    final CsInstanceRequest request = new CsInstanceRequest().withHost("foo")
    final CsInstance instance = cloud.newInstance(request);

This will create a new instance. On the new instance, an ssh keypair will be created to allow password-less login for the "user" configured for the cloud. This user will also be granted password-less sudo privileges. The hostname of the instance will be set to the hostname on the instance request + the domain name set on the cloud.

## Run a command on the instance and return the output

    cloud.execute(instance, "echo \"my username is $(whoami)\"");

## Copy files to the instance

    try (InputStream in = new FileInputStream("/some/local/file")) {
        cloud.scp(instance, in, "/destination/path/on/instance");
    }

## Destroy the instance

    cloud.teardown(instance);

# Storage

Encryption is built-in, using dataKey as the secret.

    final S3StorageEngineConfig config = new S3StorageEngineConfig();
    config.setAccessKey(accessKey);
    config.setSecretKey(secretKey);
    config.setBucket(bucket);
    config.setUsername(username);
    config.setDataKey(dataKey);  // encryption key S3 object read/write

    final CsStorageEngine storage = CsStorageEngineFactory.build(config);

    byte[] data = storage.read("/some/path");
    storage.write("/some/other/path", data);

##### License
For personal or non-commercial use, this code is available under the [GNU Affero General Public License, version 3](https://www.gnu.org/licenses/agpl-3.0.html).
For commercial use, please contact cloudstead.io
