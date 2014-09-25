package cloudos.cslib.compute.digitalocean;

import cloudos.cslib.compute.meta.CsCloudTypeBase;
import cloudos.cslib.compute.meta.CsCloudPrice;
import cloudos.cslib.compute.meta.Footprint;

public class DigitalOceanCloudType extends CsCloudTypeBase<DigitalOceanCloud> {

    public static final DigitalOceanCloudType TYPE = new DigitalOceanCloudType();

    public static final CsCloudPrice LARGE_PRICE = new CsCloudPrice().setCloudType(TYPE).setMonthlyPrice(80).setDetails("8GB memory/4 cores/80GB SSD");
    public static final CsCloudPrice MEDIUM_PRICE = new CsCloudPrice().setCloudType(TYPE).setMonthlyPrice(40).setDetails("4GB memory/2 cores/60GB SSD");
    public static final CsCloudPrice SMALL_PRICE = new CsCloudPrice().setCloudType(TYPE).setMonthlyPrice(20).setDetails("2GB memory/2 cores/40GB SSD");

    @Override
    public CsCloudPrice calculateCloudPrice(String region, Footprint footprint) {
        if (isLargeFootprint(footprint)) return LARGE_PRICE;
        if (isMediumFootprint(footprint)) return MEDIUM_PRICE;
        return SMALL_PRICE;
    }

}
