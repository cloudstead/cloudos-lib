package cloudos.model.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account", "deviceId"}, name="account_device_UNIQ_account_deviceId"))
public class AccountDevice extends IdentifiableBase {

    @Getter @Setter private String account;
    @Getter @Setter private String deviceId;
    @Getter @Setter private String deviceName;
    @Getter @Setter private Long authTime;

    public AccountDevice setAuthTime () { authTime = now(); return this; }

    public boolean isAuthYoungerThan(long deviceTimeout) {
        return authTime != null && (now() - authTime) < deviceTimeout;
    }

}
