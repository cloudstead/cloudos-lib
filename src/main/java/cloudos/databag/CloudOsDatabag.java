package cloudos.databag;

import cloudos.server.DnsConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.ApiConnectionInfo;
import rooty.toots.vendor.VendorDatabag;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.toFileOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@Accessors(chain=true)
public class CloudOsDatabag {

    public static CloudOsDatabag fromChefRepo(File dir) {
        final File databag = getDatabagFile(dir);
        if (!databag.exists()) die("fromChefRepo: databag not found: "+abs(databag));
        return fromJsonOrDie(databag, CloudOsDatabag.class);
    }

    public static File getDatabagFile(File dir) { return new File(abs(dir) + "/data_bags/cloudos/init.json"); }

    public void writeChefRepo(File dir) { toFileOrDie(getDatabagFile(dir), toJsonOrDie(this)); }

    public String getId() { return "init"; }
    public void setId (String id) { /*noop*/ }

    // UCID: universal cloud identifier, similar to UDID on a mobile device
    // uniquely identifies the cloud. multiple systems operating in concert may
    // have different hostnames but will share the same ucid
    @Getter @Setter private String ucid;

    @Getter @Setter private String server_tarball;
    public boolean hasServerTarball () { return !empty(server_tarball); }

    @Getter @Setter private int server_port;
    @Getter @Setter private String run_as;
    @Getter @Setter private String admin_initial_pass;
    @Getter @Setter private String recovery_email;
    @Getter @Setter private String aws_access_key;
    @Getter @Setter private String aws_secret_key;
    @Getter @Setter private String aws_iam_user;
    @Getter @Setter private String s3_bucket;
    @Getter @Setter private String backup_cron_schedule = "30 4  * * *";

    @Getter @Setter private ApiConnectionInfo authy = new ApiConnectionInfo();
    public CloudOsDatabag setAuthy (String baseUri, String apiKey) {
        setAuthy(new ApiConnectionInfo(baseUri, apiKey, null));
        return this;
    }

    @Getter @Setter private DnsConfiguration dns = new DnsConfiguration();
    public CloudOsDatabag setDns (String baseUri, String user, String password) {
        setDns(new DnsConfiguration(baseUri, user, password));
        return this;
    }

    @Getter @Setter private ApiConnectionInfo appstore = new ApiConnectionInfo();
    public CloudOsDatabag setAppstore (String baseUri, String ucid) {
        setAppstore(new ApiConnectionInfo(baseUri, ucid, null));
        return this;
    }

    @Getter @Setter private VendorDatabag vendor = new VendorDatabag();

}
