package cloudos.cslib.storage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsStorageEngineFactory {

    public static CsStorageEngine build(CsStorageEngineConfig config) throws Exception {
        return build(config, CsStorageEngineFactory.class.getClassLoader());
    }

    public static CsStorageEngine build(CsStorageEngineConfig config, ClassLoader classLoader) throws Exception {
        final String storageClass = config.getEngineClass();
        final CsStorageEngine engine = (CsStorageEngine) classLoader.loadClass(storageClass).newInstance();
        engine.init(config);
        return engine;
    }

}
