package cloudos.cslib.storage;

import lombok.NoArgsConstructor;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.util.string.StringUtil;

@NoArgsConstructor
public abstract class CsStorageEngineBase implements CsStorageEngine {

    protected CsStorageEngineConfig config;

    public CsStorageEngineBase(CsStorageEngineConfig config) {
        init(config);
    }

    @Override
    public void init(CsStorageEngineConfig config) {
        this.config = config;
        if (!config.hasDataKey()) throw new IllegalArgumentException("no dataKey found in config");
    }

    public String getDataKey () { return config.getDataKey(); }
    public String getBasePath () { return config.getBasePath(); }

    public String getBasePathPrefix () {
        if (config.hasBasePath()) return config.getBasePath() + "/";
        return "";
    }

    public String encryptPath (String path) throws Exception {
        return Base64.encodeBytes(CryptoUtil.encrypt((getDataKey()+path).getBytes(StringUtil.UTF8), getDataKey()));
    }

    public byte[] encrypt (byte[] data) throws Exception {
        return CryptoUtil.encrypt(data, getDataKey());
    }

    public byte[] decrypt (byte[] data) throws Exception {
        return CryptoUtil.decrypt(data, getDataKey());
    }
}
