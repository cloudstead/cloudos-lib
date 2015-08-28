package cloudos.databag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.system.PortPicker;

import java.io.File;

@NoArgsConstructor @Accessors(chain=true)
public class PortsDatabag extends Databag {

    public static final String ID = "ports";
    public String getId() { return ID; }
    public void setId (String id) { /*noop*/ }

    // chef file management functions
    public static File getChefFile(File dir, String app) { return getChefFile(PortsDatabag.class, dir, app); }
    public static PortsDatabag fromChefRepo(File dir, String app) { return fromChefRepo(PortsDatabag.class, dir, app); }
    public static PortsDatabag fromChefRepoOrNew(File dir, String app) { return fromChefRepoOrNew(PortsDatabag.class, dir, app); }

    @Getter @Setter private int primary;
    @Getter @Setter private int admin;

    public PortsDatabag (int port) { this.primary = port; }

    public static PortsDatabag pick() {
        // pick random ports
        return new PortsDatabag()
                .setPrimary(PortPicker.pickOrDie())
                .setAdmin(PortPicker.pickOrDie());
    }

}
