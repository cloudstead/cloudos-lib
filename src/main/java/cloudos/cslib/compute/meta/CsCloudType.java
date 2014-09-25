package cloudos.cslib.compute.meta;

import cloudos.cslib.compute.CsCloud;
import cloudos.cslib.compute.meta.CsCloudPrice;
import cloudos.cslib.compute.meta.Footprint;
import com.fasterxml.jackson.annotation.JsonIgnore;
import cloudos.cslib.option.CsResourceOption;

import java.util.List;
import java.util.Map;

public interface CsCloudType<T extends CsCloud> {

    @JsonIgnore public Class<T> getCloudClass();

    public String getCloudTypeName();
    public String getCloudClassName();

    // needed for ember
    public String getId();

    public List<CsResourceOption> getOptions();

    public Map<String, CsResourceOption> getOptionsMap();

    public CsCloudPrice calculateCloudPrice(String region, Footprint footprint);
}
