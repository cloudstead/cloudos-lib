package cloudos.model;

import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.security.Crypto;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Slf4j @Accessors(chain=true)
public abstract class SshKeyBase extends SshPublicKeyBase implements Scrubbable {

    public static final ScrubbableField[] SCRUBBABLE = {
            new ScrubbableField(SshKeyBase.class, "privateKey", String.class)
    };
    @Override public ScrubbableField[] fieldsToScrub() { return SCRUBBABLE; }

    public abstract Crypto getCrypto();

    @HasValue(message="err.sshkey.publicKey.required")
    @Size(max=KEY_MAXLEN, message="err.sshkey.privateKey.length")
    @Column(nullable=false, updatable=false, length=KEY_MAXLEN)
    private String privateKey;

    public String getPrivateKey() {
        return empty(privateKey) ? privateKey : getCrypto() == null ? privateKey : getCrypto().decrypt(privateKey);
    }
    public void setPrivateKey(String privateKey) {
        this.privateKey = empty(privateKey) ? privateKey : getCrypto() == null ? privateKey : getCrypto().encrypt(privateKey);
    }
}
