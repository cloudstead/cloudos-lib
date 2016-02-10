package cloudos.service.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

@NoArgsConstructor @AllArgsConstructor
public class AssetStream {

    @Getter @Setter private InputStream stream;
    @Getter @Setter private String contentType;

}
