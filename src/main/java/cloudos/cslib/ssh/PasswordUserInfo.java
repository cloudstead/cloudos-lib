package cloudos.cslib.ssh;

import com.jcraft.jsch.UserInfo;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PasswordUserInfo implements UserInfo {

    private String password;

    @Override public String getPassphrase() { return null; }
    @Override public String getPassword() { return password; }
    @Override public boolean promptPassword(String s) { return true; }
    @Override public boolean promptPassphrase(String s) { return true; }
    @Override public boolean promptYesNo(String s) { return true; }
    @Override public void showMessage(String s) {}
}
