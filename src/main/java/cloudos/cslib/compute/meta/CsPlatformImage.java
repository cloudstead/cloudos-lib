package cloudos.cslib.compute.meta;

import cloudos.model.CsPlatform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor
public class CsPlatformImage {

    @Getter @Setter private String region;
    @Getter @Setter private CsPlatform platform;
    @Getter @Setter private String image;

}
