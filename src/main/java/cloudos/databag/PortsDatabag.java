package cloudos.databag;

import lombok.Getter;
import lombok.Setter;

public class PortsDatabag {

    public String getId() { return "ports"; }
    public void setId (String id) { /*noop*/ }

    @Getter @Setter private int primary;

    public PortsDatabag (int port) { this.primary = port; }

}
