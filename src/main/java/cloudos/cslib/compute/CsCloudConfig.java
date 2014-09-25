package cloudos.cslib.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

@Accessors(chain=true)
public class CsCloudConfig {

    @Getter @Setter private String cloudClass;

    @Getter @Setter private String user;
    public boolean hasUser () { return !StringUtil.empty(user); }

    @Getter @Setter private String domain;
    public boolean hasDomain () { return !StringUtil.empty(domain); }

    public String getFqdn(String host) { return host + "." + domain; }

    @Getter @Setter private String accountId;
    @Getter @Setter private String accountSecret;
    @Getter @Setter private String groupPrefix = "";
    @Getter @Setter private String provider;
    @Getter @Setter private String image;
    @Getter @Setter private String region;
    @Getter @Setter private String zone;
    @Getter @Setter private String instanceSize;

    @JsonIgnore public boolean isValid() {
        return hasUser() && hasDomain()
                && !StringUtil.empty(accountId)
                && !StringUtil.empty(accountSecret)
                && !StringUtil.empty(provider)
                && !StringUtil.empty(region)
                && !StringUtil.empty(instanceSize);
    }

}
