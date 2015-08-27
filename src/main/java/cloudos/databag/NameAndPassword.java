package cloudos.databag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class NameAndPassword {

    @Getter @Setter private String name;
    @Getter @Setter private String password;

}
