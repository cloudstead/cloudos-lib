package cloudos.cslib.compute;

import cloudos.cslib.compute.meta.CsCloudType;
import lombok.NoArgsConstructor;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor
public class CsCloudFactory {

    public CsCloud buildCloud(CsCloudConfig config) throws Exception {

        final CsCloudType cloudType = config.getType();
        final Class<? extends CsCloud> cloudClass = cloudType.getCloudClass();
        if (empty(cloudClass)) die("config.cloudClass was missing");

        final CsCloud cloud;
        try {
            cloud = cloudClass.newInstance();
        } catch (Exception e) {
            return die("Error instantiating cloud (" + cloudType.getCloudClassName() + "): " + e, e);
        }
        cloud.init(config);
        return cloud;
    }

}
