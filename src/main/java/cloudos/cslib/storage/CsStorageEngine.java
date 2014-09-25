package cloudos.cslib.storage;

public interface CsStorageEngine {

    public void init(CsStorageEngineConfig config);

    public byte[] read(String path) throws Exception;

    public void write(String path, byte[] data) throws Exception;

}
