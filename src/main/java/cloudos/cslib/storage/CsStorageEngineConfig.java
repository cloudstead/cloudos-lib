package cloudos.cslib.storage;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;

public class CsStorageEngineConfig {

    @Getter @Setter private String engineClass;

    @Getter @Setter private String dataKey;
    public boolean hasDataKey () { return !StringUtil.empty(dataKey); }

    @Getter @Setter protected String basePath;
    public boolean hasBasePath () { return !StringUtil.empty(basePath); }

}
