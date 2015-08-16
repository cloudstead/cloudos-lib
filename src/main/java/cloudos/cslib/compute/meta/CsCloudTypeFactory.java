package cloudos.cslib.compute.meta;

import cloudos.cslib.compute.CsCloud;
import cloudos.cslib.compute.aws.AwsCloudType;
import cloudos.cslib.compute.digitalocean.DigitalOceanCloudType;
import cloudos.cslib.compute.mock.MockCloudType;
import cloudos.cslib.compute.rackspace.RackspaceCloudType;
import cloudos.model.CsGeoRegion;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class CsCloudTypeFactory {

    public static final CsCloudTypeFactory instance = new CsCloudTypeFactory();

    public static final AwsCloudType EC2 = AwsCloudType.TYPE;
    public static final DigitalOceanCloudType DO = DigitalOceanCloudType.TYPE;
    public static final RackspaceCloudType RS = RackspaceCloudType.TYPE;
    public static final MockCloudType MOCK = MockCloudType.TYPE;

    public static final CsCloudType<? extends CsCloud>[] TYPES = new CsCloudType[] { EC2, DO, RS, MOCK };

    public static final String[] PROVIDER_NAMES
            = { EC2.getProviderName(), DO.getProviderName(), RS.getProviderName(), MOCK.getProviderName() };

    @AllArgsConstructor
    public enum Type {
        ec2 (EC2), doc (DO), rs (RS);
        @Getter private CsCloudType<? extends CsCloud> type;
    }

    public CsCloudType<? extends CsCloud> fromType(String name) {
        for (CsCloudType t : TYPES) if (t.getName().equals(name)) return t;
        for (CsCloudType t : TYPES) if (t.getProviderName().equals(name)) return t;
        return die("No such cloud type: "+name);
    }

    @Getter(lazy=true) private final Map<String, List<CsGeoRegion>> regionsByCloud = initRegionsByCloud();
    private Map<String, List<CsGeoRegion>> initRegionsByCloud () {
        final Map<String, List<CsGeoRegion>> map = new HashMap<>();
        for (CsCloudType<? extends CsCloud> t : TYPES) {
            map.put(t.getName(), t.getRegions());
        }
        return map;
    }
}
