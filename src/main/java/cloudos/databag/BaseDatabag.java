package cloudos.databag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.HttpUtil;

import java.io.File;

@Accessors(chain=true)
public class BaseDatabag extends Databag {

    public static final String ID = "base";
    public static final String APP = "base";

    public String getId() { return ID; }
    public void setId (String id) { /*noop*/ }

    // chef file management
    public static File getChefFile(File dir) { return getChefFile(BaseDatabag.class, dir, APP); }
    public static BaseDatabag fromChefRepo(File dir) { return fromChefRepo(BaseDatabag.class, dir, APP); }
    public void toChefRepo(File dir) { toChefRepo(dir, APP); }

    @Getter @Setter private String hostname;
    @Getter @Setter private String parent_domain;
    @Getter @Setter private String ssl_cert_name = HttpUtil.DEFAULT_CERT_NAME;

    @JsonIgnore public String getFqdn () { return hostname + "." + parent_domain; }

}
