package cloudos.cslib.compute.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
public class CsCloudPrice {

    public CsCloudPrice(CsCloudType cloudType) { setCloudType(cloudType); }

    @Getter @Setter private CsCloudType cloudType;
    @Getter @Setter private int monthlyPrice;
    @Getter @Setter private String details;
}
