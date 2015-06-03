package cloudos.cslib.compute.rackspace;

import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.JcloudTestBase;
import org.junit.Test;

public class RackspaceCloudTest extends JcloudTestBase<RackspaceCloudType> {

    protected static final String ACCESS_KEY = TEST_PROPERTIES.getProperty("rs_client_id");
    protected static final String SECRET_KEY = TEST_PROPERTIES.getProperty("rs_api_key");

    public static final String TEST_SIZE = "general1-1";
    public static final String TEST_REGION = "IAD";
    public static final String TEST_IMAGE = "c11e2d37-bd93-44f0-b17e-bb87d1022975";

    @Test public void testHost () throws Exception { internal_testHost(); }

    @Override protected CsCloudConfig newCloudConfig() {
        final CsCloudConfig cloudConfig = super.newCloudConfig();
        cloudConfig.setAccountId(ACCESS_KEY);
        cloudConfig.setAccountSecret(SECRET_KEY);
        cloudConfig.setRegion(TEST_REGION);
        cloudConfig.setInstanceSize(TEST_SIZE);
        cloudConfig.setImage(TEST_IMAGE);
        return cloudConfig;
    }

    @Override protected RackspaceCloudType getProvider() { return RackspaceCloudType.TYPE; }

}
