package cloudos.cslib.compute.aws;

import cloudos.cslib.compute.meta.CsCloudTypeBase;
import cloudos.cslib.compute.meta.CsCloudPrice;
import cloudos.cslib.compute.meta.Footprint;
import cloudos.cslib.option.CsResourceOption;
import cloudos.cslib.option.CsResourceOptionType;
import com.google.common.collect.ImmutableMap;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.string.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AwsCloudType extends CsCloudTypeBase<AwsCloud> {

    public static final AwsCloudType TYPE = new AwsCloudType();

    public AwsCloudType () {
        setCloudClass(AwsCloud.class);
    }

    private static final String RESOURCE_PREFIX = StringUtil.getPackagePath(AwsCloudType.class);

    private static final List<String> EC2_REGIONS_LIST
            = FileUtil.loadResourceAsStringListOrDie(RESOURCE_PREFIX + "/regions.txt", AwsCloud.class);

    private static final List<String> EC2_INSTANCE_SIZES
            = FileUtil.loadResourceAsStringListOrDie(RESOURCE_PREFIX + "/instance_sizes.txt", AwsCloud.class);

    private static final Properties EC2_IMAGES_BY_REGION
            = FileUtil.resourceToPropertiesOrDie(RESOURCE_PREFIX + "/images_by_region.properties", AwsCloud.class);

    private static final CsResourceOption OPT_REGION = new CsResourceOption().setName("region")
            .setType(CsResourceOptionType.CHOICE).setChoices(EC2_REGIONS_LIST)
            .setRequired(false).setDefaultValue(EC2_REGIONS_LIST.get(0));

    private static final CsResourceOption OPT_SIZE = new CsResourceOption().setName("instanceSize")
            .setType(CsResourceOptionType.CHOICE).setChoices(EC2_INSTANCE_SIZES)
            .setRequired(false).setDefaultValue(EC2_INSTANCE_SIZES.get(0));


    // todo: figure out how to have the options available for one choice depend on another choice that's been made
//    private static final CsResourceOption OPT_ZONE = new CsResourceOption().setName(JcloudBase.K_CLOUD_ZONE)
//            .setType(CsResourceOptionType.CHOICE).setChoices(EC2_ZONES_LIST, JcloudBase.K_CLOUD_REGION)
//            .setRequired(false);

    public static String getImageForRegion (String region) {
        String image = EC2_IMAGES_BY_REGION.getProperty(region);
        if (StringUtil.empty(image)) throw new IllegalArgumentException("getImageForRegion: region argument was null");
        return region + "/" + image;
    }

    @Override
    public List<CsResourceOption> getOptions() {
        List<CsResourceOption> options = super.getOptions();
        options.add(OPT_REGION);
        options.add(OPT_SIZE);
        return options;
    }

    private static final Map<String, CsCloudPrice> LARGE_PRICES = new ImmutableMap.Builder<String, CsCloudPrice>()
            .put("us-east-1", new CsCloudPrice(TYPE).setMonthlyPrice(30 * 24 * 31).setDetails("c3.xlarge"))
            .put("us-west-1", new CsCloudPrice(TYPE).setMonthlyPrice(35 * 24 * 31).setDetails("c3.xlarge"))
            .put("us-west-2", new CsCloudPrice(TYPE).setMonthlyPrice(30 * 24 * 31).setDetails("c3.xlarge"))
            .put("sa-east-1", new CsCloudPrice(TYPE).setMonthlyPrice(80 * 24 * 31).setDetails("c1.xlarge"))
            .put("eu-west-1", new CsCloudPrice(TYPE).setMonthlyPrice(35 * 24 * 31).setDetails("c3.xlarge"))
            .put("ap-northeast-1", new CsCloudPrice(TYPE).setMonthlyPrice(38 * 24 * 31).setDetails("c3.xlarge"))
            .put("ap-southeast-1", new CsCloudPrice(TYPE).setMonthlyPrice(38 * 24 * 31).setDetails("c3.xlarge"))
            .put("ap-southeast-2", new CsCloudPrice(TYPE).setMonthlyPrice(38 * 24 * 31).setDetails("c3.xlarge"))
            .build();

    private static final Map<String, CsCloudPrice> MEDIUM_PRICES = new ImmutableMap.Builder<String, CsCloudPrice>()
            .put("us-east-1", new CsCloudPrice(TYPE).setMonthlyPrice(12 * 24 * 31).setDetails("m3.medium"))
            .put("us-west-1", new CsCloudPrice(TYPE).setMonthlyPrice(12 * 24 * 31).setDetails("m3.medium"))
            .put("us-west-2", new CsCloudPrice(TYPE).setMonthlyPrice(13 * 24 * 31).setDetails("m3.medium"))
            .put("sa-east-1", new CsCloudPrice(TYPE).setMonthlyPrice(16 * 24 * 31).setDetails("m3.medium"))
            .put("eu-west-1", new CsCloudPrice(TYPE).setMonthlyPrice(13 * 24 * 31).setDetails("m3.medium"))
            .put("ap-northeast-1", new CsCloudPrice(TYPE).setMonthlyPrice(17 * 24 * 31).setDetails("m3.medium"))
            .put("ap-southeast-1", new CsCloudPrice(TYPE).setMonthlyPrice(16 * 24 * 31).setDetails("m3.medium"))
            .put("ap-southeast-2", new CsCloudPrice(TYPE).setMonthlyPrice(16 * 24 * 31).setDetails("m3.medium"))
            .build();

    private static final Map<String, CsCloudPrice> SMALL_PRICES = new ImmutableMap.Builder<String, CsCloudPrice>()
            .put("us-east-1", new CsCloudPrice(TYPE).setMonthlyPrice(2 * 24 * 31).setDetails("t1.micro"))
            .put("us-west-1", new CsCloudPrice(TYPE).setMonthlyPrice(3 * 24 * 31).setDetails("t1.micro"))
            .put("us-west-2", new CsCloudPrice(TYPE).setMonthlyPrice(2 * 24 * 31).setDetails("t1.micro"))
            .put("sa-east-1", new CsCloudPrice(TYPE).setMonthlyPrice(3 * 24 * 31).setDetails("t1.micro"))
            .put("eu-west-1", new CsCloudPrice(TYPE).setMonthlyPrice(2 * 24 * 31).setDetails("t1.micro"))
            .put("ap-northeast-1", new CsCloudPrice(TYPE).setMonthlyPrice(3 * 24 * 31).setDetails("t1.micro"))
            .put("ap-southeast-1", new CsCloudPrice(TYPE).setMonthlyPrice(2 * 24 * 31).setDetails("t1.micro"))
            .put("ap-southeast-2", new CsCloudPrice(TYPE).setMonthlyPrice(2 * 24 * 31).setDetails("t1.micro"))
            .build();

    @Override
    public CsCloudPrice calculateCloudPrice(String region, Footprint footprint) {
        // todo: this should be more dynamic & live-data driven.
        // for now, simple table lookups will suffice. but prices will change.
        // perhaps fetch latest prices from the app store?
        if (isLargeFootprint(footprint)) return LARGE_PRICES.get(region);
        if (isMediumFootprint(footprint)) return MEDIUM_PRICES.get(region);
        return SMALL_PRICES.get(region);
    }

}