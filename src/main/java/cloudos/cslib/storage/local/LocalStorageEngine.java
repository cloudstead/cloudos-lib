package cloudos.cslib.storage.local;

import cloudos.cslib.storage.CsStorageEngineBase;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class LocalStorageEngine extends CsStorageEngineBase {

    @Override
    public byte[] read (String path) throws Exception {
        return decrypt(FileUtil.toBytes(new File(getBasePath(), encryptPath(path))));
    }

    @Override
    public void write (String path, byte[] data) throws Exception {
        File f = new File(getBasePath(), encryptPath(path));
        data = encrypt(data);
        try (OutputStream out = new FileOutputStream(f)) {
            IOUtils.copy(new ByteArrayInputStream(data), out);
        }
    }

}
