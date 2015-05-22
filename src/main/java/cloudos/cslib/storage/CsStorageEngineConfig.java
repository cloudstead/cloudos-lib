package cloudos.cslib.storage;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class CsStorageEngineConfig {

    @Getter @Setter private String engineClass;

    @Getter @Setter private String dataKey;
    public boolean hasDataKey () { return !empty(dataKey); }

    @Getter @Setter protected String basePath;
    public boolean hasBasePath () { return !empty(basePath); }

}
