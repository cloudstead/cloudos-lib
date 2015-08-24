package cloudos.cslib.compute;

import cloudos.cslib.compute.meta.CsCloudType;
import cloudos.cslib.compute.meta.CsCloudTypeFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

// Do NOT enable @Accessors(chain=true) for this class.
// This class is used by YML files, which cannot populate beans using that pattern.
@EqualsAndHashCode
public class CsCloudConfig {

    @JsonIgnore @Getter @Setter private CsCloudType<? extends CsCloud> type;

    // For yaml/json constructors, so they don't have to deal with the "type" variable directly
    public String getProvider () { return type.getProviderName(); }
    public void setProvider (String name) { type = CsCloudTypeFactory.instance.fromType(name); }

    @Getter @Setter private String user;
    public boolean hasUser () { return !empty(user); }

    @Getter @Setter private String domain;
    public boolean hasDomain () { return !empty(domain); }

    public String getFqdn(String host) { return host + "." + domain; }

    @Getter @Setter private String accountId;
    public boolean hasAccountId () { return !empty(accountId); }

    @Getter @Setter private String accountSecret;
    @Getter @Setter private String groupPrefix = "";

    @Getter @Setter private String image;
    @Getter @Setter private String region;
    @Getter @Setter private String zone;
    @Getter @Setter private String instanceSize;

    @JsonIgnore public boolean isValid() {
        return hasUser() && hasDomain()
                && !empty(accountId)
                && !empty(accountSecret)
                && !empty(type)
                && !empty(region)
                && !empty(instanceSize);
    }

}
