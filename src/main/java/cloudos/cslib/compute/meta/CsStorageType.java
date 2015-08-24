package cloudos.cslib.compute.meta;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CsStorageType {

    local, block;

    @JsonCreator public static CsStorageType create(String n) { return valueOf(n.toLowerCase()); }

}
