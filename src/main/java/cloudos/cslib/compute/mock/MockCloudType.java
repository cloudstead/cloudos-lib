package cloudos.cslib.compute.mock;

import cloudos.cslib.compute.meta.CsCloudTypeBase;

public class MockCloudType extends CsCloudTypeBase<MockCloud> {

    public static final MockCloudType TYPE = new MockCloudType();

    @Override public String getProviderName() { return "mock"; }

    @Override protected String getRegionsJson() { return null; }

    @Override protected String getInstanceTypesJson() { return null; }

    @Override protected String getPlatformImagesJson() { return null; }

}