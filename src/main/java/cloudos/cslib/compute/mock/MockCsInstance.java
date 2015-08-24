package cloudos.cslib.compute.mock;

import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import cloudos.model.instance.CloudOsState;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MockCsInstance extends CsInstance {

    public static final String LOCALHOST = "127.0.0.1";
    public static final HashSet<String> PUBLIC_ADDRESSES = new HashSet<>(Arrays.asList(LOCALHOST));

    @Getter @Setter private MockCloud mockCloud;
    @Getter @Setter private CloudOsState state;
    @Getter @Setter private CsInstanceRequest request;

    public MockCsInstance(MockCloud mockCloud, CsInstanceRequest request) {
        setMockCloud(mockCloud);
        setRequest(request);
        setState(CloudOsState.initial);
    }

    @Override public Set<String> getPublicAddresses() { return PUBLIC_ADDRESSES; }

    @Override public String getPublicIp() { return LOCALHOST; }


}
