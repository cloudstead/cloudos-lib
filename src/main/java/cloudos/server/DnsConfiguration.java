package cloudos.server;

import cloudos.databag.CloudOsDnsMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.http.ApiConnectionInfo;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor
public class DnsConfiguration extends ApiConnectionInfo {

    @Getter @Setter private CloudOsDnsMode mode;
    @Getter @Setter private boolean enabled = true;
    @Getter @Setter private String account;
    @Getter @Setter private String zone;

    public DnsConfiguration(String account, String zone, String user, String password) {
        super(null, user, password);
        this.account = account;
        this.zone = zone;
    }

    public DnsConfiguration(String baseUri, String user, String password) { super(baseUri, user, password); }

    public boolean isDynDns () { return mode == CloudOsDnsMode.dyn; }

    public boolean isValid () {
        return isDynDns()
                ? !empty(getUser()) && !empty(getPassword()) && !empty(getAccount()) && !empty(getZone())
                : !empty(getUser()) && !empty(getPassword()) && !empty(getBaseUri());
    }
}
