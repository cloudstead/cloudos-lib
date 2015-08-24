package cloudos.cslib.compute.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum CsStorageMedium {

    ssd (CsStorageType.local),
    hdd (CsStorageType.local),
    block (CsStorageType.block);

    @Getter private CsStorageType type;

    @JsonCreator public static CsStorageMedium create(String n) { return valueOf(n.toLowerCase()); }

}
