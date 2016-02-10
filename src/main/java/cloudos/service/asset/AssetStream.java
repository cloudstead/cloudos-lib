package cloudos.service.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

@NoArgsConstructor @AllArgsConstructor
public class AssetStream {

    @Getter @Setter private String uri;
    @Getter @Setter private InputStream stream;
    @Getter @Setter private String contentType;

    public final static String[][] FORMAT_MAP = {
            {"png", "png"},
            {"jpg", "jpg"},
            {"jpeg", "jpg"},
            {"gif", "gif"},
    };
    @JsonIgnore public String getFormatName() {
        for (String[] format : FORMAT_MAP) {
            if (contentType.contains("/"+format[0])) return format[1];
        }
        return null;
    }
}
