package cloudos.model.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account", "deviceId"}))
public class AccountDevice extends IdentifiableBase {

    @Getter @Setter private String account;
    @Getter @Setter private String deviceId;
    @Getter @Setter private String deviceName;
    @Getter @Setter private Long authTime;

    public AccountDevice setAuthTime () { authTime = System.currentTimeMillis(); return this; }

    public boolean isAuthYoungerThan(long deviceTimeout) {
        return authTime != null && (System.currentTimeMillis() - authTime) < deviceTimeout;
    }

}
