package cloudos.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import static org.cobbzilla.util.string.StringUtil.empty;

public enum CsPlatform {

    ubuntu_14_lts;

    @JsonCreator public static CsPlatform create (String val) { return empty(val) ? null : valueOf(val.toLowerCase()); }

}
