package cloudos.cslib.ssh;

import com.jcraft.jsch.UserInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.io.FileUtil;

import java.io.IOException;

@Accessors(chain=true)
public class CsJschConnectionInfo implements UserInfo {

    @Override public String getPassword() { return getPassphrase(); }
    @Override public boolean promptPassword(String s) { return true; }
    @Override public boolean promptPassphrase(String s) { return true; }
    @Override public boolean promptYesNo(String s) { return true; }
    @Override public void showMessage(String s) {}

    @Getter @Setter private String user;
    @Getter @Setter private String host;
    @Getter @Setter private int port = 22;
    @Getter @Setter private String key;
    public CsJschConnectionInfo setKeyFile(String key) throws IOException {this.key = FileUtil.toString(key); return this;}
    public boolean getHasKey () { return key != null; }
    public byte[] getKeyBytes () { return key.getBytes(); }

    // If the key is not null, this is the passphrase for the key. Otherwise, this is the password for the user.
    @Getter @Setter private String passphrase;
    public byte[] getPassphraseBytes () { return passphrase == null ? null : passphrase.getBytes(); }

}
