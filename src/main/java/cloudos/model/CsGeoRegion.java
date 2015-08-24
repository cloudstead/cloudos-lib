package cloudos.model;

import cloudos.cslib.compute.CsCloud;
import cloudos.cslib.compute.meta.CsCloudType;
import cloudos.cslib.compute.meta.CsCloudTypeFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
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

    @HasValue(message="{err.geoRegion.vendor.empty}")
    @JsonIgnore @Getter @Setter private CsCloudType<? extends CsCloud> cloudVendor;

    public String getVendor () { return cloudVendor.getName(); }
    public void setVendor (String name) { setCloudVendor(CsCloudTypeFactory.instance.fromType(name)); }

    @HasValue(message="{err.geoRegion.name.empty}")
    @Getter @Setter private String name;  // unique within a cloud vendor
    @Getter @Setter private String country;
    @Getter @Setter private String region;

    public String getImage(String instanceType, CsPlatform platform) {
        return cloudVendor.getImage(getInstanceType(instanceType), platform, name);
    }

    protected CsInstanceType getInstanceType(String instanceType) {
        return cloudVendor.getInstanceType(instanceType);
    }

    @JsonIgnore public boolean isValid() {
        if (empty(cloudVendor) || empty(name)) return false;
        for (CsGeoRegion r : cloudVendor.getRegions()) {
            if (r.getName().equals(name)) return true;
        }
        return false;
    }

}
