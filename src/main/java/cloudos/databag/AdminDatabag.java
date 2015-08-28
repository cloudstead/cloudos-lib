package cloudos.databag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class AdminDatabag extends Databag {

    public static final String ID = "admin";

    public String getId() { return ID; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private String name;
    @Getter @Setter private String email;

}
