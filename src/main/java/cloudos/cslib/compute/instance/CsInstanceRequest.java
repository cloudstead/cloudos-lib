package cloudos.cslib.compute.instance;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomStringUtils;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true)
public class CsInstanceRequest {

    // Just the simple hostname (not FQDN). The domain comes from the cloud that launches it.
    // If null, a random hostname will be created.
    @Setter private String host;

    public String getHost () {
        if (empty(host)) {
            host = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
        }
        return host;
    }

}
