package cloudos.cslib.compute.aws;

import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.JcloudTestBase;
import org.junit.Test;

public class AwsCloudTest extends JcloudTestBase<AwsCloudType> {

    protected static final String ACCESS_KEY = TEST_PROPERTIES.getProperty("access_key");
    protected static final String SECRET_KEY = TEST_PROPERTIES.getProperty("secret_key");

    public static final String TEST_SIZE = "t1.micro";
    public static final String TEST_REGION = "us-east-1";

    @Test public void testHost () throws Exception { internal_testHost(); }

    @Override
    protected CsCloudConfig newCloudConfig() {
        final CsCloudConfig cloudConfig = super.newCloudConfig();
        cloudConfig.setAccountId(ACCESS_KEY);
        cloudConfig.setAccountSecret(SECRET_KEY);
        cloudConfig.setRegion(TEST_REGION);
        cloudConfig.setInstanceSize(TEST_SIZE);
        return cloudConfig;
    }

    @Override protected AwsCloudType getProvider() { return AwsCloudType.TYPE; }

}
