package cloudos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.cobbzilla.util.daemon.ZillaRuntime;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public enum CsNetworkLevel {

    low, moderate, high, ten_gigabit;

    @JsonCreator public static CsNetworkLevel create (String val) { return empty(val) ? null : valueOf(val.toLowerCase()); }

}
