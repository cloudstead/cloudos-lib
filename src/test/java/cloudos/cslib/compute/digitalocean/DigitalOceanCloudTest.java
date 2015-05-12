package cloudos.cslib.compute.digitalocean;

import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.JcloudTestBase;
import org.junit.Test;

public class DigitalOceanCloudTest extends JcloudTestBase<DigitalOceanCloudType> {

    protected static final String ACCESS_KEY = TEST_PROPERTIES.getProperty("do_client_id");
    protected static final String SECRET_KEY = TEST_PROPERTIES.getProperty("do_api_key");
    protected static final String TEST_REGION = "sfo1";
    protected static final String TEST_SIZE = "512mb";
    protected static final String TEST_IMAGE = "ubuntu-14-04-x64";

    @Test public void testHost () throws Exception { internal_testHost(); }

    @Override
    protected CsCloudConfig newCloudConfig() {
        final CsCloudConfig cloudConfig = super.newCloudConfig();
        cloudConfig.setAccountId(ACCESS_KEY);
        cloudConfig.setAccountSecret(SECRET_KEY);
        cloudConfig.setRegion(TEST_REGION);
        cloudConfig.setInstanceSize(TEST_SIZE);
        cloudConfig.setImage(TEST_IMAGE);
        return cloudConfig;
    }

    @Override protected DigitalOceanCloudType getProvider() { return DigitalOceanCloudType.TYPE; }

}
