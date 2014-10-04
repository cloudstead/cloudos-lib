package cloudos.model.auth;

import cloudos.model.AccountBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;

@NoArgsConstructor @AllArgsConstructor
public abstract class AuthResponse<T extends AccountBase> {

    @Getter @Setter private String sessionId;
    @Getter @Setter private T account;

    public boolean hasSessionId() { return !StringUtil.empty(sessionId) && !isTwoFactor(); }

    public static final String TWO_FACTOR_SID = "2-factor";

    @JsonIgnore public boolean isTwoFactor () { return TWO_FACTOR_SID.equals(sessionId); }

}
