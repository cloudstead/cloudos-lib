package cloudos.cslib.compute.mock;

import cloudos.cslib.compute.CsCloudBase;
import cloudos.cslib.compute.CsCloudConfig;
import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Slf4j
public class MockCloud extends CsCloudBase {

    @Override
    public void init(CsCloudConfig config) {
        log.info("init: "+config);
        this.config = config;
    }


    @Override
    public CsInstance newInstance(CsInstanceRequest request) throws Exception {
        return new MockCsInstance(this, request);
    }

    @Override
    public boolean teardown(CsInstance instance) throws Exception {
        return true;
    }

    @Override
    public String execute(CsInstance instance, String command) throws Exception {
        return null;
    }

    @Override
    public boolean scp(CsInstance instance, InputStream in, String remotePath) throws Exception {
        return true;
    }

    @Override
    public boolean ssh(CsInstance instance) throws Exception {
        return true;
    }
}
