package cloudos.cslib.compute.rackspace;

import cloudos.cslib.compute.CsCloudBase;
import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;

public class RackspaceCloud extends CsCloudBase {

    @Override public CsInstance newInstance(CsInstanceRequest request) throws Exception {
        // todo
        return null;
    }

    @Override public boolean teardown(CsInstance instance) throws Exception {
        // todo
        return false;
    }

}
