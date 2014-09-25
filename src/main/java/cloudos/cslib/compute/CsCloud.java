package cloudos.cslib.compute;

import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;

import java.io.InputStream;

public interface CsCloud {

    public void init (CsCloudConfig config);

    public CsCloudConfig getConfig();

    public CsInstance newInstance(CsInstanceRequest request) throws Exception;

    public boolean teardown(CsInstance instance) throws Exception;

    public String execute(CsInstance instance, String command) throws Exception;
    public boolean scp(CsInstance instance, InputStream in, String remotePath) throws Exception;
    public boolean ssh(CsInstance instance) throws Exception;

}
