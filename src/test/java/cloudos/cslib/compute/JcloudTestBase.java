package cloudos.cslib.compute;

public abstract class JcloudTestBase extends CsCloudTestBase {

    private static final String TEST_PREFIX = "cslib-test-";

    protected CsCloudConfig newCloudConfig() {
        return new CsCloudConfig()
                .setProvider(getProvider())
                .setGroupPrefix(TEST_PREFIX)
                .setCloudClass(getCloudClass());
    }

    protected abstract String getProvider();
    protected abstract String getCloudClass();

}
