package cloudos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.cobbzilla.util.security.MD5Util;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.StringReader;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Accessors(chain=true)
public class SslCertificateBase extends UniquelyNamedEntity {

    public static final String CN_PREFIX = "CN=";

    @Transient public String getCertName () { return getName(); }
    public void setCertName (String name) { setName(name); }

    @Getter @Setter private String description;
    @Getter @Setter private String commonName;
    @Getter @Setter private String pemSha;
    @Getter @Setter private String pemMd5;
    @Getter @Setter private String keySha;
    @Getter @Setter private String keyMd5;

    public SslCertificateBase setPem(String pem) throws Exception {
        pemSha = ShaUtil.sha256_hex(pem);
        pemMd5 = MD5Util.md5hex(pem);

        // validate PEM
        final PEMParser pemParser = new PEMParser(new StringReader(pem));
        Object thing;
        while ((thing = pemParser.readObject()) != null) {
            if (thing instanceof X509CertificateHolder) {
                final String subject = ((X509CertificateHolder) thing).getSubject().toString();
                if (subject != null) {
                    if (subject.startsWith(CN_PREFIX) && subject.length() > CN_PREFIX.length()) {
                        commonName = subject.substring(CN_PREFIX.length());

                    } else if (subject.contains(CN_PREFIX)) {
                        final int start = subject.indexOf(CN_PREFIX) + CN_PREFIX.length();
                        int commaPos = subject.indexOf(",", start);
                        if (commaPos == -1) commaPos = subject.length();
                        commonName = subject.substring(start, commaPos);
                    }
                }
            }
        }
        if (empty(commonName)) {
            die("No common name found in certificate");
        }
        return this;
    }

    public SslCertificateBase setKey(String key) {
        keySha = ShaUtil.sha256_hex(key);
        keyMd5 = MD5Util.md5hex(key);
        return this;
    }

    @JsonIgnore public boolean isValid() { return false; }

    public boolean isValidForHostname(String hostname) {
        if (empty(commonName)) die("isValidForHostname: commonName was not set");
        if (commonName.startsWith("*.")) {
            return hostname.endsWith(commonName.substring(1));
        }
        return hostname.equals(commonName);
    }

}
