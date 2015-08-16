package cloudos.cslib.compute.meta;

import cloudos.cslib.compute.CsCloud;
import cloudos.cslib.option.CsResourceOption;
import cloudos.model.CsGeoRegion;
import cloudos.model.CsInstanceType;
import cloudos.model.CsPlatform;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public interface CsCloudType<T extends CsCloud> {

    @JsonIgnore public Class<T> getCloudClass();

    public String getName();
    public String getCloudClassName();

    // the jclouds provider name
    public String getProviderName();

    // needed for ember
    public String getId();

    public List<CsResourceOption> getOptions();
    public Map<String, CsResourceOption> getOptionsMap();

    public List<CsGeoRegion> getRegions ();
    public CsGeoRegion getRegion(String name);

    public List<CsInstanceType> getInstanceTypes ();

    public CsInstanceType getSmallestInstanceType ();

    public CsInstanceType getInstanceType(String name);

    public String getImage(CsPlatform platform, String region);
}
