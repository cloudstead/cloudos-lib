package cloudos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.cobbzilla.util.daemon.ZillaRuntime;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public enum CsPlatform {

    ubuntu_14_lts;

    @JsonCreator public static CsPlatform create (String val) { return empty(val) ? null : valueOf(val.toLowerCase()); }

}
