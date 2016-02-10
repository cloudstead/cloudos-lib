package cloudos.service.asset;

import cloudos.server.asset.AssetStorageConfiguration;
import cloudos.server.asset.AssetStorageType;

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

    public abstract InputStream load(String uri);
    public abstract boolean exists(String uri);
    public abstract String store(InputStream fileStream);

}
