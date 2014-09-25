package cloudos.cslib.compute.aws;

import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.JcloudTestBase;
import cloudos.cslib.compute.jclouds.JcloudBase;
import org.junit.Test;

public class AwsCloudTest extends JcloudTestBase {

    protected static final String ACCESS_KEY = TEST_PROPERTIES.getProperty("access_key");
    protected static final String SECRET_KEY = TEST_PROPERTIES.getProperty("secret_key");

    public static final String TEST_SIZE = "t1.micro";
    public static final String TEST_REGION = "us-east-1";

    @Test public void testHost () throws Exception { internal_testHost(); }

    @Override
    protected CsCloudConfig newCloudConfig() {
        return super.newCloudConfig()
                .setAccountId(ACCESS_KEY)
                .setAccountSecret(SECRET_KEY)
                .setRegion(TEST_REGION)
                .setInstanceSize(TEST_SIZE);
    }

    @Override protected String getProvider() { return JcloudBase.PROVIDER_AWS_EC2; }
    @Override protected String getCloudClass() { return AwsCloud.class.getName(); }

}
