package cloudos.cslib.compute;

import cloudos.cslib.compute.meta.CsCloudType;
import lombok.NoArgsConstructor;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@NoArgsConstructor
public class CsCloudFactory {

    public CsCloud buildCloud(CsCloudConfig config) throws Exception {

        final CsCloudType cloudType = config.getType();
        final Class<? extends CsCloud> cloudClass = cloudType.getCloudClass();
        if (empty(cloudClass)) die("config.cloudClass was missing");

        final CsCloud cloud = instantiate(cloudClass);
        cloud.init(config);
        return cloud;
    }

}
