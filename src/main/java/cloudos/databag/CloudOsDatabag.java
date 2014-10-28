package cloudos.databag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.ApiConnectionInfo;
import rooty.toots.vendor.VendorDatabag;

@Accessors(chain=true)
public class CloudOsDatabag {

    public String getId() { return "init"; }
    public void setId (String id) { /*noop*/ }

    // UCID: universal cloud identifier, similar to UDID on a mobile device
    // uniquely identifies the cloud. multiple systems operating in concert may
    // have different hostnames but will share the same ucid
    @Getter @Setter private String ucid;

    @Getter @Setter private String server_tarball;
    @Getter @Setter private int server_port;
    @Getter @Setter private String run_as;
    @Getter @Setter private String admin_initial_pass;
    @Getter @Setter private String recovery_email;
    @Getter @Setter private String aws_access_key;
    @Getter @Setter private String aws_secret_key;
    @Getter @Setter private String aws_iam_user;
    @Getter @Setter private String s3_bucket;

    @Getter @Setter private ApiConnectionInfo authy = new ApiConnectionInfo();
    public CloudOsDatabag setAuthy (String baseUri, String apiKey) {
        setAuthy(new ApiConnectionInfo(baseUri, apiKey, null));
        return this;
    }

    @Getter @Setter private ApiConnectionInfo dns = new ApiConnectionInfo();
    public CloudOsDatabag setDns (String baseUri, String user, String password) {
        setDns(new ApiConnectionInfo(baseUri, user, password));
        return this;
    }

    @Getter @Setter private VendorDatabag vendor = new VendorDatabag();
}
