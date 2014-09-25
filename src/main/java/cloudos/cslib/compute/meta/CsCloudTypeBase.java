package cloudos.cslib.compute.meta;

import cloudos.cslib.compute.CsCloud;
import cloudos.cslib.option.CsResourceOption;
import cloudos.cslib.option.CsResourceOptionType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CsCloudTypeBase<T extends CsCloud> implements CsCloudType<T> {

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

    @Getter @Setter protected Class<T> cloudClass;

    public String getCloudTypeName () { return getClass().getSimpleName(); }
    public String getCloudClassName () { return cloudClass.getName(); }

    // needed for ember
    public String getId() { return getClass().getSimpleName(); }

    public List<CsResourceOption> getOptions() {
        List<CsResourceOption> list = new ArrayList<>();
        list.add(OPT_NAME);
        list.add(OPT_CLOUD_USER);
        list.add(OPT_ACCOUNT_IDENTIFIER);
        list.add(OPT_ACCOUNT_SECRET);
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

    protected boolean isLargeFootprint(Footprint footprint) {
        return footprint != null && footprint.getCpus() >= 10;
    }

    protected boolean isMediumFootprint(Footprint footprint) {
        return footprint != null && footprint.getCpus() >= 4;
    }
}
