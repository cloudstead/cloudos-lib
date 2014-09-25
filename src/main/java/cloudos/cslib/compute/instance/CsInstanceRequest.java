package cloudos.cslib.compute.instance;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.RandomStringUtils;
import org.cobbzilla.util.string.StringUtil;

@Accessors(chain=true)
public class CsInstanceRequest {

    // Just the simple hostname (not FQDN). The domain comes from the cloud that launches it.
    // If null, a random hostname will be created.
    @Setter private String host;

    public String getHost () {
        if (StringUtil.empty(host)) {
            host = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
        }
        return host;
    }

}
