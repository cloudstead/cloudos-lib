package cloudos.cslib.compute.rackspace;

import cloudos.cslib.compute.meta.CsCloudTypeBase;

public class RackspaceCloudType extends CsCloudTypeBase<RackspaceCloud> {

    public static final RackspaceCloudType TYPE = new RackspaceCloudType();

    @Override public String getProviderName() { return "rackspace-cloudservers-us"; }

    @Override protected String getRegionsJson() { return "cloudos/cslib/rs/regions.json"; }
    @Override protected String getInstanceTypesJson() { return "cloudos/cslib/rs/instance_types.json"; }
    @Override protected String getPlatformImagesJson() { return "cloudos/cslib/rs/platform_images.json"; }

}
