package cloudos.service.asset;

import com.amazonaws.services.s3.internal.Mimetypes;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.FileUtil;

import java.io.*;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.*;

@Slf4j
public class LocalAssetStorageService extends AssetStorageService {

    public static final String PROP_BASE = "baseDir";

    public static final String APPLICATION_JSON = "application/json";

    @Getter @Setter private File baseDir;
    @Getter @Setter private String contentType;

    public LocalAssetStorageService(Map<String, String> config) {
        String basePath = (config == null) ? null : config.get(PROP_BASE);
        if (empty(basePath)) basePath = System.getProperty("java.io.tmpdir");
        this.baseDir = FileUtil.mkdirOrDie(basePath);
    }

    @Override public AssetStream load(String uri) {
        try {
            final String path = abs(baseDir) + "/" + uri;
            final File f = new File(path);
            return f.exists() ? new AssetStream(uri, new FileInputStream(f), getContentType(path)) : null;

        } catch (FileNotFoundException e) {
            log.warn("load: "+e);
            return null;
        }
    }

    public String getContentType(String path) {
        return contentType != null ? contentType : toStringOrDie(abs(path)+".contentType");
    }

    @Override public boolean exists(String uri) { return new File(abs(baseDir) + "/" + uri).exists(); }

    @Override public String store(InputStream fileStream, String filename, String path) {
        final String mimeType = getMimeType(filename);
        try {
            final File temp = File.createTempFile("localAsset", ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                IOUtils.copyLarge(fileStream, out);
            }
            if (path == null) path = getUri(temp, filename);
            final File stored = new File(abs(baseDir) + "/" + path);
            mkdirOrDie(stored.getParentFile());

            // rename, or copy, or die
            if (!temp.renameTo(stored)) {
                FileUtils.copyFile(temp, stored);
                FileUtils.deleteQuietly(temp);
            }
            if (contentType == null) FileUtil.toFile(abs(stored)+".contentType", mimeType);

            return path;

        } catch (Exception e) {
            return die("store: "+e, e);
        }
    }

    public String getMimeType(String filename) {
        return filename.endsWith(".json") ? APPLICATION_JSON : Mimetypes.getInstance().getMimetype(filename);
    }
}
