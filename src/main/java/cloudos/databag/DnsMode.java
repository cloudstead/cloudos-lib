package cloudos.databag;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DnsMode {

    dyn, cdns, internal;

    @JsonCreator public static DnsMode create (String val) { return valueOf(val.toLowerCase()); }

}