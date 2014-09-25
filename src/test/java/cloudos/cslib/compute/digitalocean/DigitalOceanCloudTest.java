package cloudos.cslib.compute.digitalocean;

import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.JcloudTestBase;
import cloudos.cslib.compute.jclouds.JcloudBase;
import org.junit.Test;

public class DigitalOceanCloudTest extends JcloudTestBase {

    protected static final String ACCESS_KEY = TEST_PROPERTIES.getProperty("do_client_id");
    protected static final String SECRET_KEY = TEST_PROPERTIES.getProperty("do_api_key");
    protected static final String TEST_REGION = "sfo1";
    protected static final String TEST_SIZE = "512mb";
    protected static final String TEST_IMAGE = "ubuntu-14-04-x64";

    @Test public void testHost () throws Exception { internal_testHost(); }

    @Override
    protected CsCloudConfig newCloudConfig() {
        return super.newCloudConfig()
                .setAccountId(ACCESS_KEY)
                .setAccountSecret(SECRET_KEY)
                .setRegion(TEST_REGION)
                .setInstanceSize(TEST_SIZE)
                .setImage(TEST_IMAGE);
    }

    @Override protected String getProvider() { return JcloudBase.PROVIDER_DIGITALOCEAN; }
    @Override protected String getCloudClass() { return DigitalOceanCloud.class.getName(); }

}
