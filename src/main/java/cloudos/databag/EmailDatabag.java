package cloudos.databag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.system.ConnectionInfo;
import rooty.toots.vendor.VendorDatabag;

@Accessors(chain=true)
public class EmailDatabag {

    public String getId() { return "init"; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private ConnectionInfo smtp_relay = new ConnectionInfo();
    @Getter @Setter private VendorDatabag vendor = new VendorDatabag();

    public EmailDatabag setSmtpRelay (String username, String password, String host, int port) {
        setSmtp_relay(new ConnectionInfo(host, port, username, password));
        return this;
    }
}
