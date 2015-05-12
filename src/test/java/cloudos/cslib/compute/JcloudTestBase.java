package cloudos.cslib.compute;

import cloudos.cslib.compute.meta.CsCloudType;

public abstract class JcloudTestBase<T extends CsCloudType> extends CsCloudTestBase {

    private static final String TEST_PREFIX = "cslib-test-";

    protected CsCloudConfig newCloudConfig() {
        final CsCloudConfig cloudConfig = new CsCloudConfig();
        cloudConfig.setType(getProvider());
        cloudConfig.setGroupPrefix(TEST_PREFIX);
        return cloudConfig;
    }

    protected abstract T getProvider();

}
