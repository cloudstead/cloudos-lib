package cloudos.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true)
public class LoginRequest {

    @Setter private String name;
    public String getName () { return name == null ? null : name.toLowerCase(); }

    @Getter @Setter @JsonProperty private String password;
    @Getter @Setter @JsonProperty private String secondFactor;
    @JsonIgnore public boolean isSecondFactor () { return !empty(secondFactor); }

    @Getter @Setter private String deviceId;
    @JsonIgnore public boolean hasDevice () { return !empty(deviceId); }

    @Getter @Setter private String deviceName;

    public String toString () {
        return "{name="+getName()+", password="+mask(password)+", secondFactor="+mask(secondFactor)+", device="+getDevice()+"}";
    }

    @JsonIgnore public String getDevice() { return hasDevice() ? deviceId + " ("+deviceName+")" : "NOT-SET"; }

    public String mask(String value) { return empty(value) ? "NOT-SET" : "SET"; }
}
