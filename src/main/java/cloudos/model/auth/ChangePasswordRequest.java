package cloudos.model.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public class ChangePasswordRequest {

    @Getter @Setter private String uuid;
    @Getter @Setter private String oldPassword;
    @Getter @Setter private String newPassword;

}
