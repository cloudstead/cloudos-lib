package cloudos.cslib.compute;

import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.ssh.CsJsch;
import cloudos.cslib.ssh.CsJschConnectionInfo;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.system.CommandShell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class CsCloudBase implements CsCloud {

    private static final long SSH_EXEC_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    @Getter protected CsCloudConfig config;

    @Override
    public void init (CsCloudConfig config) {
        if (!config.isValid()) throw new IllegalArgumentException("invalid config: "+config);
        this.config = config;
    }

    public String execute(CsInstance instance, String command) throws Exception {
        @Cleanup final CsJsch jsch = getJsch(instance);
        return jsch.exec(command, SSH_EXEC_TIMEOUT);
    }

    public boolean scp(CsInstance instance, InputStream in, String remotePath) throws Exception {
        @Cleanup("delete") File temp = toTempFile(in);
        return scp(instance, temp, remotePath);
    }

    private File toTempFile(InputStream in) throws IOException {
        final File temp = StreamUtil.stream2temp(in);
        CommandShell.chmod(temp, "600");
        return temp;
    }

    public boolean scp(CsInstance instance, InputStream in, String remotePath, String user, String key, String passphrase) throws Exception {
        @Cleanup("delete") File temp = toTempFile(in);
        @Cleanup final CsJsch jsch = getJsch(instance, user, key, passphrase);
        return scp(jsch, temp, remotePath);
    }

    public boolean scp(CsInstance instance, File file, String remotePath) throws Exception {
        @Cleanup final CsJsch jsch = getJsch(instance);
        return scp(jsch, file, remotePath);
    }

    private boolean scp(CsJsch jsch, File file, String remotePath) throws Exception {
        return jsch.scpTo(file, remotePath, SSH_EXEC_TIMEOUT);
    }

    public boolean ssh(CsInstance instance) throws Exception {
        @Cleanup final CsJsch jsch = getJsch(instance);
        return jsch.ssh(SSH_EXEC_TIMEOUT);
    }

    protected CsJsch getJsch(CsInstance instance) {
        final CsJschConnectionInfo identity = new CsJschConnectionInfo()
                .setHost(instance.getPublicIp())
                .setPort(instance.getPort())
                .setUser(instance.getUser())
                .setKey(instance.getKey())
                .setPassphrase(instance.getPassphrase());
        return new CsJsch(identity);
    }

    protected CsJsch getJsch(CsInstance instance, String user, String privateKey, String passphrase) {
        final CsJschConnectionInfo identity = new CsJschConnectionInfo()
                .setHost(instance.getPublicIp())
                .setPort(instance.getPort())
                .setUser(user)
                .setKey(privateKey)
                .setPassphrase(passphrase);
        return new CsJsch(identity);
    }

}
