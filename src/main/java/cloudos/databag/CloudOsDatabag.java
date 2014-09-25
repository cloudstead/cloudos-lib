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
