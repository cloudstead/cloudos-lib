package cloudos.cslib.compute.digitalocean;

import cloudos.cslib.compute.meta.CsCloudTypeBase;

public class DigitalOceanCloudType extends CsCloudTypeBase<DigitalOceanCloud> {

    public static final DigitalOceanCloudType TYPE = new DigitalOceanCloudType();

    @Override public String getProviderName() { return "digitalocean"; }

    @Override protected String getRegionsJson() { return "cloudos/cslib/do/regions.json"; }
    @Override protected String getInstanceTypesJson() { return "cloudos/cslib/do/instance_types.json"; }
    @Override protected String getPlatformImagesJson() { return "cloudos/cslib/do/platform_images.json"; }

}
