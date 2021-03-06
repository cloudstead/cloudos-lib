package cloudos.cslib.compute.meta;

import cloudos.cslib.compute.CsCloud;
import cloudos.cslib.option.CsResourceOption;
import cloudos.cslib.option.CsResourceOptionType;
import cloudos.model.CsGeoRegion;
import cloudos.model.CsInstanceType;
import cloudos.model.CsPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.JsonUtil;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;

public abstract class CsCloudTypeBase<T extends CsCloud> implements CsCloudType<T> {

    public static final ObjectMapper JSON = JsonUtil.FULL_MAPPER_ALLOW_COMMENTS;

    public static final CsResourceOption OPT_NAME = new CsResourceOption()
            .setName("name").setType(CsResourceOptionType.TEXT)
            .setRequired(false).freeze();

    public static final CsResourceOption OPT_CLOUD_USER = new CsResourceOption()
            .setName("user").setType(CsResourceOptionType.TEXT)
            .setRequired(false).freeze();

    public static final CsResourceOption OPT_ACCOUNT_IDENTIFIER = new CsResourceOption()
            .setName("accountId").setType(CsResourceOptionType.TEXT)
            .setRequired(true).freeze();

    public static final CsResourceOption OPT_ACCOUNT_SECRET = new CsResourceOption()
            .setName("accountSecret").setType(CsResourceOptionType.TEXT)
            .setRequired(true).freeze();
    public static final String WILDCARD = "*";

    @Override public Class<T> getCloudClass() { return getFirstTypeParam(getClass()); }

    @Getter(lazy=true) private final List<CsGeoRegion> regions = initRegions();
    protected List<CsGeoRegion> initRegions() {
        final List<CsGeoRegion> list;
        try {
            final String json = StreamUtil.loadResourceAsString(getRegionsJson());
            list = Arrays.asList(JSON.readValue(json, CsGeoRegion[].class));
        } catch (Exception e) {
            return die("Error loading regions json from classpath ("+getRegionsJson()+"): "+e, e);
        }
        for (CsGeoRegion r : list) r.setCloudVendor(this);
        return list;
    }
    protected abstract String getRegionsJson();

    @Override public CsGeoRegion getRegion(String name) {
        for (CsGeoRegion region : getRegions()) {
            if (region.getName().equalsIgnoreCase(name)) return region;
        }
        return null;
    }

    public List<String> getRegionNames () {
        final List<String> names = new ArrayList<>();
        for (CsGeoRegion r : getRegions()) names.add(r.getName());
        return names;
    }

    @Getter(lazy=true) private final List<CsInstanceType> instanceTypes = initInstanceTypes();
    protected List<CsInstanceType> initInstanceTypes() {
        try {
            final String json = StreamUtil.loadResourceAsString(getInstanceTypesJson());
            return Arrays.asList(JSON.readValue(json, CsInstanceType[].class));
        } catch (Exception e) {
            return die("Error loading instanceTypes json from classpath ("+getInstanceTypesJson()+"): "+e, e);
        }
    }
    protected abstract String getInstanceTypesJson();

    @Override public CsInstanceType getInstanceType(String name) {
        for (CsInstanceType t : getInstanceTypes()) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return die("No such instance type: '" + name + "'");
    }
    public CsInstanceType type(String name) { return getInstanceType(name); }

    @Override public CsInstanceType getSmallestInstanceType() {
        return getInstanceTypes().get(0);
    }

    public List<String> getInstanceTypeNames () {
        final List<String> names = new ArrayList<>();
        for (CsInstanceType t : getInstanceTypes()) names.add(t.getName());
        return names;
    }

    @Getter(lazy=true) private final List<CsPlatformImage> platformImages = initPlatformImage();
    private List<CsPlatformImage> initPlatformImage() {
        try {
            final String json = StreamUtil.loadResourceAsString(getPlatformImagesJson());
            return Arrays.asList(JSON.readValue(json, CsPlatformImage[].class));
        } catch (Exception e) {
            return die("Error loading platformImages json from classpath ("+getPlatformImagesJson()+"): "+e, e);
        }
    }
    protected abstract String getPlatformImagesJson();

    @Override public String getImage(CsInstanceType instanceType, CsPlatform platform, String region) {
        for (CsPlatformImage image : getPlatformImages()) {
            if (image.getPlatform().equals(platform)
                    && regionMatches(image.getRegion(), region)
                    && isCompatible(instanceType, image)) {
                return image.getImage();
            }
        }
        return die("No image found for region/platform: "+region+"/"+platform+" (available: "+getPlatformImages()+")");
    }

    protected boolean isCompatible(CsInstanceType instanceType, CsPlatformImage image) {
        return instanceType == null
                || image.getRegion().equals(WILDCARD)
                || instanceType.getStorage_type().equals(image.getStorage_type());
    }

    private boolean regionMatches(String cloudRegion, String requestedRegion) {
        return cloudRegion.equals(WILDCARD) || cloudRegion.equals(requestedRegion);
    }

    public String getName() { return getClass().getSimpleName(); }
    public String getCloudClassName () { return getFirstTypeParam(getClass()).getName(); }

    // needed for ember
    public String getId() { return getName(); }

    public List<CsResourceOption> getOptions() {
        List<CsResourceOption> list = new ArrayList<>();
        list.add(OPT_NAME);
        list.add(OPT_CLOUD_USER);
        list.add(OPT_ACCOUNT_IDENTIFIER);
        list.add(OPT_ACCOUNT_SECRET);

        final List<String> instanceTypeNames = getInstanceTypeNames();
        if (!empty(instanceTypeNames)) {
            list.add(new CsResourceOption().setName("instanceSize")
                    .setType(CsResourceOptionType.CHOICE).setChoices(instanceTypeNames)
                    .setRequired(false).setDefaultValue(instanceTypeNames.get(0)));
        }

        final List<String> regionNames = getRegionNames();
        if (!empty(regionNames)) {
            list.add(new CsResourceOption().setName("region")
                    .setType(CsResourceOptionType.CHOICE).setChoices(regionNames)
                    .setRequired(false).setDefaultValue(regionNames.get(0)));
        }

        return list;
    }

    @Override
    public Map<String, CsResourceOption> getOptionsMap() {
        Map<String, CsResourceOption> map = new LinkedHashMap<>();
        for (CsResourceOption option : getOptions()) {
            map.put(option.getName(), option);
        }
        return map;
    }

}
