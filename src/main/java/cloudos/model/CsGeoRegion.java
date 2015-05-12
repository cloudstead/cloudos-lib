package cloudos.model;

import cloudos.cslib.compute.meta.CsCloudType;
import cloudos.cslib.compute.meta.CsCloudTypeFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class CsGeoRegion {

    // construct from JSON
    public CsGeoRegion (String json) {
        final CsGeoRegion parsed = fromJsonOrDie(json, getClass());
        this.setName(parsed.getName());
        this.setCountry(parsed.getCountry());
        this.setRegion(parsed.getRegion());
        this.setVendor(parsed.getVendor());
    }

    @JsonIgnore @Getter @Setter private CsCloudType cloudVendor;

    public String getVendor () { return cloudVendor.getName(); }
    public void setVendor (String name) { setCloudVendor(CsCloudTypeFactory.instance.fromType(name)); }

    @Getter @Setter private String name;
    @Getter @Setter private String country;
    @Getter @Setter private String region;

    public String getImage(CsPlatform platform) { return cloudVendor.getImage(platform, name); }

}
