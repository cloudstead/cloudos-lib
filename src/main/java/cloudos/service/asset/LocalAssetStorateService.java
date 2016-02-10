package cloudos.service.asset;

import com.amazonaws.services.s3.internal.Mimetypes;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.security.ShaUtil;

import java.io.*;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.mkdirOrDie;
import static org.cobbzilla.util.io.FileUtil.toStringOrDie;

public class LocalAssetStorateService extends AssetStorageService {

    private static final String PROP_BASE = "baseDir";

    @Getter @Setter private File baseDir;

    public LocalAssetStorateService(Map<String, String> config) {
        String basePath = (config == null) ? null : config.get(PROP_BASE);
        if (empty(basePath)) basePath = System.getProperty("java.io.tmpdir");
        this.baseDir = FileUtil.mkdirOrDie(basePath);
    }

    @Override public AssetStream load(String uri) {
        try {
            final String path = abs(baseDir) + uri;
            return new AssetStream(new FileInputStream(path), toStringOrDie(abs(path)+".contentType"));

        } catch (FileNotFoundException e) {
            return die("load: "+e, e);
        }
    }

    @Override public boolean exists(String uri) { return new File(abs(baseDir) + uri).exists(); }

    @Override public String store(InputStream fileStream, String filename) {
        final String mimeType = Mimetypes.getInstance().getMimetype(filename);
        final String ext = FileUtil.extension(filename);
        try {
            final File temp = File.createTempFile("localAsset", ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                IOUtils.copyLarge(fileStream, out);
            }
            final String sha = ShaUtil.sha256_file(temp);
            final String path = sha.substring(0, 2) + "/" + sha.substring(2, 4) + "/" + sha.substring(4, 6) + "/" + sha + ext;
            final File stored = new File(abs(baseDir) + "/" + path);
            mkdirOrDie(stored.getParentFile());
            if (!temp.renameTo(stored)) die("store: error renaming "+abs(temp)+" -> "+abs(stored));
            FileUtil.toFile(abs(stored)+".contentType", mimeType);
            return path;

        } catch (Exception e) {
            return die("store: "+e, e);
        }
    }
}
