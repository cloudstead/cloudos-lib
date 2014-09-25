package cloudos.cslib.compute;

import lombok.NoArgsConstructor;
import org.cobbzilla.util.string.StringUtil;

@NoArgsConstructor
public class CsCloudFactory {

    public CsCloud buildCloud(CsCloudConfig config) throws Exception {

        final String cloudClass = config.getCloudClass();
        if (StringUtil.empty(cloudClass)) throw new IllegalArgumentException("config.cloudClass was missing");

        final CsCloud cloud;
        try {
            cloud = (CsCloud) Class.forName(cloudClass).newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error instantiating cloud ("+cloudClass+"): "+e, e);
        }
        cloud.init(config);
        return cloud;
    }

}
