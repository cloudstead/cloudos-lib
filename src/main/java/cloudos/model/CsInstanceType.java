package cloudos.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class CsInstanceType {

    @Getter @Setter private String name;
    @Getter @Setter private int memory;
    @Getter @Setter private int vcpu;
    @Getter @Setter private int gpu;
    @Getter @Setter private int storage;
    @Getter @Setter private String storage_medium;
    @Getter @Setter private String storage_geometry;
    @Getter @Setter private String storage_bandwidth;
    @Getter @Setter private CsNetworkLevel networking;

}
