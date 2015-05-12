package cloudos.cslib.compute.aws;

import cloudos.cslib.compute.meta.CsCloudTypeBase;

public class AwsCloudType extends CsCloudTypeBase<AwsCloud> {

    public static final AwsCloudType TYPE = new AwsCloudType();

    @Override public String getProviderName() { return "aws-ec2"; }

    @Override protected String getRegionsJson() { return "cloudos/cslib/aws/regions.json"; }
    @Override protected String getInstanceTypesJson() { return "cloudos/cslib/aws/instance_types.json"; }
    @Override protected String getPlatformImagesJson() { return "cloudos/cslib/aws/platform_images.json"; }

    // todo: figure out how to have the options available for one choice depend on another choice that's been made
//    private static final CsResourceOption OPT_ZONE = new CsResourceOption().setName(JcloudBase.K_CLOUD_ZONE)
//            .setType(CsResourceOptionType.CHOICE).setChoices(EC2_ZONES_LIST, JcloudBase.K_CLOUD_REGION)
//            .setRequired(false);

}