package cloudos.model.instance;

import cloudos.cslib.compute.instance.CsInstance;
import cloudos.model.CsGeoRegion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.Crypto;
import org.cobbzilla.util.security.CryptoSimple;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.*;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass
@ToString(of={"adminUuid", "name", "state"})
@Accessors(chain=true)
public class CloudOsBase extends UniquelyNamedEntity {

    @Getter @Setter private String adminUuid;

    // Name has a lot of restrictions: must have a value; min 3/max 30 alphanumeric chars; cannot be reserved word
    @Size(max=30, message="err.cloudos.name.length")
    @Pattern(regexp = "[A-Za-z0-9]{3,}", message = "err.cloudos.name.invalid")
    @Column(updatable=false, unique=true, length=30)
    public String getName () { return name == null ? null : name.toLowerCase(); }
    public CloudOsBase setName (String n) {
        if (isReserved(n)) throw invalidEx("err.cloudos.name.reserved");
        name = (n == null) ? null : n.toLowerCase(); return this;
    }

    protected boolean isReserved(String n) { return false; }

    @NotNull(message="err.cloudos.instanceType.required")
    @Column(length=30, nullable=false)
    @Getter @Setter private String instanceType;

    @NotNull(message="err.cloudos.region.required")
    @Column(length=200, nullable=false)
    @JsonIgnore
    @Getter @Setter private String region;

    @Transient
    public CsGeoRegion getCsRegion () { return fromJsonOrDie(region, CsGeoRegion.class); }
    public CloudOsBase setCsRegion (CsGeoRegion r) { region = toJsonOrDie(r); return this; }

    @Size(max=4096, message="err.cloudos.apps.length")
    @Getter @Setter private String apps;

    public List<String> getAllApps() {
        return empty(apps)
                ? new ArrayList<>(CloudOsAppBundle.required.getApps())
                : parseApps();
    }

    protected List<String> parseApps() {
        final String[] apps = this.apps.split("[,\\s]+");
        final List<String> list = new ArrayList<>();
        for (String app : apps) {
            if (CloudOsAppBundle.isValid(app)) {
                list.addAll(CloudOsAppBundle.valueOf(app).getApps());
            } else {
                list.add(app);
            }
        }
        return list;
    }

    @NotNull @Enumerated(value=EnumType.STRING) @Column(length=30, nullable=false)
    @Getter @Setter private CloudOsState state = CloudOsState.initial;
    @Getter @Setter private long lastStateChange;

    @JsonIgnore public boolean isLaunchable() { return state == CloudOsState.initial; }
    @JsonIgnore public boolean isDestroyable() { return state != CloudOsState.initial; }

    @JsonIgnore public boolean isRunning() { return state == CloudOsState.live; }

    @Column(length=1024, updatable=false, unique=true)
    @JsonIgnore @Getter @Setter private String stagingDir;
    @JsonIgnore public boolean hasStagingDir () { return !empty(stagingDir); }
    @JsonIgnore public File getStagingDirFile () { return hasStagingDir() ? new File(stagingDir) : null; }

    public void updateState (CloudOsState newState) {
        if (newState != state) {
            state = newState;
            lastStateChange = System.currentTimeMillis();
        }
    }

    @Column(nullable=false, updatable=false, unique=true, length=100)
    @Getter @Setter private String ucid;
    public void initUcid () { if (empty(ucid)) this.ucid = UUID.randomUUID().toString(); }

    @Transient @JsonIgnore
    public Crypto getCrypto () { return new CryptoSimple(ShaUtil.sha256_hex(getAdminUuid())); }

    @Size(max=64000, message="err.cloudos.instanceJson.tooLong")
    @JsonIgnore private String instanceJson;

    public String getInstanceJson() {
        final Crypto crypto = getCrypto();
        return empty(instanceJson) || crypto == null ? instanceJson : crypto.decrypt(instanceJson);
    }

    public void setInstanceJson(String instanceJson) {
        final Crypto crypto = getCrypto();
        this.instanceJson = crypto == null ? instanceJson : crypto.encrypt(instanceJson);
    }

    @JsonIgnore public CsInstance getInstance () {
        try {
            return empty(instanceJson) ? null : JsonUtil.fromJson(getInstanceJson(), CsInstance.class);
        } catch (Exception e) {
            return die("Invalid instanceJson: " + e, e);
        }
    }

    // for backing up instance configuration and data to S3
    // all backups are encrypted on the instance with the end-user's master password
    // so while the cloudstead itself can read the data, it will be indecipherable to the launcher
    @Getter @Setter @JsonIgnore private String s3accessKey;
    @Getter @Setter @JsonIgnore private String s3secretKey;

    public String getIAMpath(Identifiable admin) {
        return "/cloudos/" + admin.getUuid().replace("-", "") + "/";
    }

    public String getIAMuser(Identifiable admin, String hostname, String salt) {
        return admin.getUuid().replace("-", "") + "_" + sha256_hex(hostname + salt).substring(0, 10);
    }

    public File initStagingDir(File dir) {
        if (!empty(stagingDir)) return getStagingDirFile();
        final File stagingDirFile = mkdirOrDie(createTempDirOrDie(dir, getName() + "_chef_"));
        stagingDir = abs(stagingDirFile);
        return stagingDirFile;
    }

    @Transient @JsonIgnore public boolean canTerminate() { return true; }

}
