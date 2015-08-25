package cloudos.databag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.http.HttpUtil;

@Accessors(chain=true)
public class BaseDatabag {

    public String getId() { return "base"; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private String hostname;
    @Getter @Setter private String parent_domain;
    @Getter @Setter private String ssl_cert_name = HttpUtil.DEFAULT_CERT_NAME;

}
