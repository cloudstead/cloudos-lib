package cloudos.databag;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CloudOsDnsMode {

    dyn, external, internal;

    @JsonCreator public static CloudOsDnsMode create (String val) { return valueOf(val.toLowerCase()); }

}