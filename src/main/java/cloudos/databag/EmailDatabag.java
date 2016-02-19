package cloudos.databag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.system.ConnectionInfo;
import org.cobbzilla.util.chef.VendorDatabag;

@Accessors(chain=true)
public class EmailDatabag extends Databag {

    public String getId() { return "init"; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private ConnectionInfo smtp_relay = new ConnectionInfo();
    @Getter private EmailServerType server_type = EmailServerType.custom;

    public void setServer_type(EmailServerType server_type) {
        this.server_type = server_type;
        smtp_relay.setHost(server_type.getHost());
        smtp_relay.setPort(server_type.getPort());
    }

    @Getter @Setter private VendorDatabag vendor = new VendorDatabag();
    @Getter @Setter private String vmail_user = "vmail";
    @Getter @Setter private String postmaster_user = "postmaster";

    public EmailDatabag setSmtpRelay (String username, String password, String host, int port) {
        return setSmtp_relay(new ConnectionInfo(host, port, username, password));
    }

    public EmailDatabag setSmtpRelay (ConnectionInfo connection) {
        return setSmtp_relay(connection);
    }
}
