package cloudos.model.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public class ResetPasswordRequest {

    @Getter @Setter private String token;
    @Getter @Setter private String password;

}
