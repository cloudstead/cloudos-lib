package cloudos.databag;

import lombok.Getter;
import lombok.Setter;

public class UserDatabag {

    @Getter @Setter private String login;
    @Getter @Setter private String password;
    @Getter @Setter private String email;
    @Getter @Setter private boolean admin;

}
