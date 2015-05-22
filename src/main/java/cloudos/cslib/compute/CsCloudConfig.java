package cloudos.cslib.compute;

import cloudos.cslib.compute.meta.CsCloudType;
import cloudos.cslib.compute.meta.CsCloudTypeFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class CsCloudConfig {

    @Getter @Setter private CsCloudType<? extends CsCloud> type;

    // For yaml-style constructors
    public void setProvider (String name) {
        type = CsCloudTypeFactory.instance.fromType(name);
    }

    @Getter @Setter private String user;
    public boolean hasUser () { return !empty(user); }

    @Getter @Setter private String domain;
    public boolean hasDomain () { return !empty(domain); }

    public String getFqdn(String host) { return host + "." + domain; }

    @Getter @Setter private String accountId;
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
