package cloudos.cslib.storage.s3;

import cloudos.cslib.storage.CsStorageEngineConfig;
import com.amazonaws.auth.AWSCredentials;
import lombok.Getter;
import lombok.Setter;

public class S3StorageEngineConfig extends CsStorageEngineConfig implements AWSCredentials {

    @Getter @Setter private String accessKey;
    @Getter @Setter private String secretKey;
    @Getter @Setter private String bucket;
    @Getter @Setter private String username;

    @Override public String getEngineClass() { return S3StorageEngine.class.getName(); }
    @Override public String getAWSAccessKeyId() { return getAccessKey(); }
    @Override public String getAWSSecretKey() { return getSecretKey(); }

}
