package cloudos.cslib.compute.mock;

import cloudos.cslib.compute.CsCloudBase;
import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import cloudos.cslib.ssh.CsKeyPair;
import cloudos.model.instance.CloudOsState;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Slf4j
public class MockCloud extends CsCloudBase {

    @Override public void init(CsCloudConfig config) { this.config = config; }

    @Override public CsInstance newInstance(CsInstanceRequest request) throws Exception {
        return new MockCsInstance(this, request);
    }

    @Override public int teardown(CsInstance instance) throws Exception {
        ((MockCsInstance) instance).setState(CloudOsState.destroyed);
        return 1;
    }

    @Override public CsInstance findInstance(String instanceId, String name, CsKeyPair keyPair) {
        return null;
    }

    @Override public boolean isRunning(CsInstance instance) throws Exception {
        final CloudOsState state = ((MockCsInstance) instance).getState();
        return state != CloudOsState.initial && state != CloudOsState.destroyed;
    }

    @Override public String execute(CsInstance instance, String command) throws Exception {
        return null;
    }

    @Override public boolean scp(CsInstance instance, InputStream in, String remotePath) throws Exception {
        return true;
    }

    @Override public boolean ssh(CsInstance instance) throws Exception {
        return true;
    }
}
