package cloudos.cslib.storage.s3;

import cloudos.cslib.storage.CsStorageEngineBase;
import cloudos.cslib.storage.CsStorageEngineConfig;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.cobbzilla.util.http.HttpStatusCodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
public class S3StorageEngine extends CsStorageEngineBase {

    private AmazonS3Client s3Client;

    @Override
    public void init(CsStorageEngineConfig config) {

        if (!(config instanceof S3StorageEngineConfig)) throw new IllegalArgumentException("config is not an instance of S3StorageEngineConfig: "+config);
        super.init(config);

        s3Client = new AmazonS3Client(getS3Config());
    }

    private S3StorageEngineConfig getS3Config() {
        return (S3StorageEngineConfig) config;
    }

    private String getPath(String path) throws Exception {
        return getBasePathPrefix() + getS3Config().getUsername()+ "/" + encryptPath(path);
    }

    @Override
    public byte[] read(String path) throws Exception {
        final S3Object object;
        try {
            object = s3Client.getObject(getS3Config().getBucket(), getPath(path));
        } catch (AmazonS3Exception e) {
            log.info("read error: "+e);
            if (e.getStatusCode() == HttpStatusCodes.NOT_FOUND) {
                return null;
            }
            throw e;
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        @Cleanup final InputStream in = object.getObjectContent();
        IOUtils.copy(in, out);
        return decrypt(out.toByteArray());
    }

    @Override
    public void write(String path, byte[] data) throws Exception {
        s3Client.putObject(getS3Config().getBucket(), getPath(path), new ByteArrayInputStream(encrypt(data)), null);
    }

}
