package cloudos.service.asset;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.security.ShaUtil;

import java.io.*;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.mkdirOrDie;

public class S3AssetStorageService extends AssetStorageService {

    public static final String PROP_ACCESS_KEY = "accessKey";
    public static final String PROP_SECRET_KEY = "secretKey";
    public static final String PROP_BUCKET = "bucket";
    public static final String PROP_PREFIX = "prefix";
    public static final String PROP_LOCAL_CACHE = "localCache";

    @Getter @Setter private String accessKey;
    @Getter @Setter private String secretKey;
    @Getter @Setter private String bucket;
    @Getter @Setter private String prefix;
    @Getter @Setter private File localCache;

    private final AmazonS3Client s3Client;

    public S3AssetStorageService(Map<String, String> config) {
        setAccessKey(config.get(PROP_ACCESS_KEY));
        setSecretKey(config.get(PROP_SECRET_KEY));
        setBucket(config.get(PROP_BUCKET));
        setPrefix(config.get(PROP_PREFIX));

        String local = config.get(PROP_LOCAL_CACHE);
        if (empty(local)) local = System.getProperty("java.io.tmpdir");
        setLocalCache(mkdirOrDie(local));

        s3Client = new AmazonS3Client(new AWSCredentials() {
            @Override public String getAWSAccessKeyId() { return getAccessKey(); }
            @Override public String getAWSSecretKey() { return getSecretKey(); }
        });
    }

    @Override public InputStream load(String uri) {
        final File cachefile = new File(abs(localCache) + "/" + uri);
        if (cachefile.exists()) try {
            return new FileInputStream(cachefile);
        } catch (FileNotFoundException e) {
            die("load: "+e, e);
        }
        return s3Client.getObject(bucket, prefix + "/" + uri).getObjectContent();
    }

    @Override public boolean exists(String uri) {
        if (new File(abs(localCache) + "/" + uri).exists()) return true;
        try {
            s3Client.getObject(bucket, prefix + "/" + uri);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override public String store(InputStream fileStream) {
        try {
            final File temp = File.createTempFile("s3Asset", ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                IOUtils.copyLarge(fileStream, out);
            }
            final String sha = ShaUtil.sha256_file(temp);
            final String path = sha.substring(0, 2) + "/" + sha.substring(2, 4) + "/" + sha.substring(4, 6) + "/" + sha;
            final File stored = new File(abs(localCache) + "/" + path);
            if (exists(path)) {
                FileUtils.deleteQuietly(temp);

            } else {
                mkdirOrDie(stored.getParentFile());
                if (!temp.renameTo(stored)) die("store: error renaming " + abs(temp) + " -> " + abs(stored));
                s3Client.putObject(bucket, prefix + "/" + path, stored);
            }
            return path;

        } catch (Exception e) {
            return die("store: "+e, e);
        }
    }
}
