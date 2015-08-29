package cloudos.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass @Slf4j @Accessors(chain=true)
public class SshPublicKeyBase extends UniquelyNamedEntity {

    public static final int KEY_MAXLEN = 8192;

    @HasValue(message="err.sshkey.privateKey.required")
    @Size(max=KEY_MAXLEN, message="err.sshkey.privateKey.length")
    @Column(nullable=false, updatable=false, length=KEY_MAXLEN)
    @Getter @Setter private String publicKey;

}
