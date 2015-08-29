package cloudos.cslib.compute.instance;

import cloudos.model.SshKeyBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true)
public class CsInstanceRequest {

    // Just the simple hostname (not FQDN). The domain comes from the cloud that launches it.
    // If null, a random hostname will be created.
    @Setter private String host;

    public String getHost () {
        if (empty(host)) {
            host = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
        }
        return host;
    }

    // If not null, this key will be put in the authorized_keys file for the user
    @Getter @Setter private String publicKey;
    public boolean hasPublicKey () { return !empty(publicKey); }

    @JsonIgnore public boolean isDsaKey () { return hasPublicKey() && publicKey.toLowerCase().trim().startsWith(SshKeyBase.DSA_KEY); }
    @JsonIgnore public boolean isRsaKey () { return hasPublicKey() && publicKey.toLowerCase().trim().startsWith(SshKeyBase.RSA_KEY); }

    @JsonIgnore public InputStream getPublicKeyStream() {
        return new ByteArrayInputStream(getPublicKey().getBytes());
    }

    public String getInstallKeyCommand(String keyPath) {
        final String authFile = isDsaKey() ? "~/.ssh/authorized_keys2" : "~/.ssh/authorized_keys";
        return "bash -c 'mkdir -p ~/.ssh && cat "+keyPath+" >> "+authFile+" && chmod 600 "+authFile + " && rm "+keyPath+"'";
    }
}
