package cloudos.cslib.compute.instance;

import cloudos.cslib.compute.CsCloudConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class CsInstance {

    @Getter @Setter private CsCloudConfig cloudConfig;

    @Getter @Setter private String host;
    @Getter @Setter private int port;
    @Getter @Setter private String user;
    @Getter @Setter @JsonIgnore private String key;
    @Getter @Setter @JsonIgnore private String passphrase;
    @Getter @Setter private String group;

    @Getter @Setter private String vendorId;
    @Getter @Setter private String image;
    @Getter @Setter private String region;
    @Getter @Setter private String zone;
    @Getter @Setter private String instanceSize;

    @Getter @Setter private Set<String> publicAddresses;
    @Getter @Setter private Set<String> privateAddresses;

    @JsonIgnore public String getPublicIp() {
        return publicAddresses == null || publicAddresses.isEmpty() ? null : publicAddresses.iterator().next();
    }

}
