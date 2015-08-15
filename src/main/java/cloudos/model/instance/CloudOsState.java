package cloudos.model.instance;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CloudOsState {

    initial, retrying, starting, started, cheffing, cheffed, setup_complete, live, error, destroying, destroyed, deleting;

    @JsonCreator public CloudOsState create (String name) { return valueOf(name.toLowerCase()); }

    public static CloudOsState[] INACTIVE = { initial, destroying, destroyed, deleting };
}
