package cloudos.databag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
public class PortsDatabag {

    public static final String ID = "ports";

    public String getId() { return ID; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private int primary;
    @Getter @Setter private int admin;

    public PortsDatabag (int port) { this.primary = port; }

}
