package cloudos.databag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.HttpUtil;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

@Accessors(chain=true)
public class BaseDatabag {

    public static BaseDatabag fromChefRepo(File dir) {
        final File databag = new File(abs(dir) + "/data_bags/cloudos/base.json");
        if (!databag.exists()) die("fromChefRepo: base databag not found: "+abs(databag));
        return fromJsonOrDie(databag, BaseDatabag.class);
    }

    public String getId() { return "base"; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private String hostname;
    @Getter @Setter private String parent_domain;
    @Getter @Setter private String ssl_cert_name = HttpUtil.DEFAULT_CERT_NAME;
    @Getter @Setter private CloudOsDnsMode dns_mode;

}
