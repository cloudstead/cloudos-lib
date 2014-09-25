package cloudos.cslib.compute;

import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
import org.junit.After;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class CsCloudTestBase {

    protected static final Properties TEST_PROPERTIES = FileUtil.toPropertiesOrDie(System.getProperty("user.home") + File.separator + ".cslib.config");

    protected static final String CLOUD_USER = System.getProperty("user.name");
    public static final String TEST_DOMAIN = "example.com";

    public static final CsCloudFactory cloudFactory = new CsCloudFactory();

    protected CsCloud cloud;
    protected CsInstance instance;

    protected abstract CsCloudConfig newCloudConfig();

    public void internal_testHost () throws Exception {
        final CsCloud cloud = getCloud();
        instance = newInstance(cloud);

        // Verify we can execute a command as the regular user
        final String tracer = RandomStringUtils.randomAlphanumeric(20);
        final String actual = cloud.execute(instance, "echo " + tracer).trim();
        assertEquals(tracer, actual);
    }

    protected CsCloud getCloud() throws Exception {
        final CsCloudConfig cloudConfig = newCloudConfig();
        cloudConfig.setUser(CLOUD_USER);
        cloudConfig.setDomain(TEST_DOMAIN);
        cloud = cloudFactory.buildCloud(cloudConfig);
        assertNotNull(cloud);
        return cloud;
    }

    protected CsInstance newInstance(CsCloud cloud) throws Exception {
        instance = cloud.newInstance(new CsInstanceRequest());
        assertNotNull(instance);
        return instance;
    }

    @After
    public void teardownInstance () throws Exception {
        if (instance != null) cloud.teardown(instance);
    }

}
