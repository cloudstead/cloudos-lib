package cloudos.service.asset;

import cloudos.server.asset.AssetStorageConfiguration;
import cloudos.server.asset.AssetStorageType;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.security.ShaUtil;

import java.io.File;
import java.io.InputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public abstract class AssetStorageService {

    public static AssetStorageService build(AssetStorageConfiguration configuration) {
        AssetStorageType type = configuration.getType();
        if (type == null) type = AssetStorageType.local;
        switch (type) {
            case local: return new LocalAssetStorateService(configuration.getConfig());
            case s3: return new S3AssetStorageService(configuration.getConfig());
            default: return die("AssetStorageService.build: invalid type: "+ type);
        }
    }

    public abstract AssetStream load(String uri);

    public abstract boolean exists(String uri);

    public String store(InputStream fileStream, String filename) { return store(fileStream, filename, null); }

    public abstract String store(InputStream fileStream, String fileName, String uri);

    public String getUri(File file, String filename) {
        final String sha = ShaUtil.sha256_file(file);
        return sha.substring(0, 2) + "/" + sha.substring(2, 4) + "/" + sha.substring(4, 6) + "/" + sha + FileUtil.extension(filename);
    }
}
