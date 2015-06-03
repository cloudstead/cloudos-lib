package cloudos.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class CsInstanceType {

    @Getter @Setter private String name;

    @Getter @Setter private double memory; // in GB
    @Getter @Setter private int vcpu;
    @Getter @Setter private int gpu;

    @Getter @Setter private double system_storage;   // in GB, if there is a separate system disk
    @Getter @Setter private double storage;   // in GB
    @Getter @Setter private String storage_medium; // ssd, hdd or block
    @Getter @Setter private String storage_geometry;
    @Getter @Setter private double storage_bandwidth; // in Mb/s

    @Getter @Setter private CsNetworkLevel networking;
    @Getter @Setter private double network_bandwidth; // in Mb/s

}
