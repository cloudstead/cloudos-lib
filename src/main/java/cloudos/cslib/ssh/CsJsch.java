package cloudos.cslib.ssh;

import com.jcraft.jsch.*;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cobbzilla.util.string.StringUtil;

import java.io.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.string.StringUtil.UTF8;

// some code borrowed from examples on http://www.jcraft.com/jsch/
@Slf4j
public class CsJsch {

    public static final String PUB = ".pub";
    public static final String DEFAULT_COMMENT = "no comment";

    private static final byte[] NULL_TERMINATOR = {0};
    public static final JSch JSCH = new JSch();

    static {
        JSch.setLogger(new Logger() {
            @Override public boolean isEnabled(int i) { return true; }
            @Override public void log(int level, String message) {
                switch (level) {
                    case DEBUG: log.debug(message); break;
                    case INFO: log.info(message); break;
                    case WARN: log.warn(message); break;
                    case ERROR: log.error(message); break;
                    case FATAL: log.error("(FATAL): "+message); break;
                    default: log.error("(UNKNOWN LEVEL: "+level+"): "+message); break;
                }
            }
        });
    }

    private CsJschConnectionInfo identity;
    private Session session;

    public CsJsch(CsJschConnectionInfo identity) {
        this.identity = identity;
    }

    public Channel open(String mode, int timeout) throws Exception {
        if (session == null) {
            session = JSCH.getSession(identity.getUser(), identity.getHost(), identity.getPort());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey");
            UserInfo userInfo;
            if (identity.getHasKey()) {
                JSCH.addIdentity(identity.getUser(), identity.getKeyBytes(), null, identity.getPassphraseBytes());
                userInfo = identity;
            } else if (identity.getPassword() != null) {
                userInfo = new PasswordUserInfo(identity.getPassword());
            } else {
                throw new IllegalArgumentException("config had neither key nor password");
            }
            session.setUserInfo(userInfo);
            session.connect(timeout);
        }

        return session.openChannel(mode);
    }

    public void close () {
        session.disconnect();
    }

    public static void generateKeyPair (OutputStream privateOut, OutputStream publicOut) throws Exception {
        generateKeyPair(privateOut, publicOut, null, null);
    }

    public static void generateKeyPair (OutputStream privateOut, OutputStream publicOut, String passphrase) throws Exception {
        generateKeyPair(privateOut, publicOut, passphrase, null);
    }

    public static void generateKeyPair (OutputStream privateOut, OutputStream publicOut, String passphrase, String comment) throws Exception {
        @Cleanup("dispose") final KeyPair kpair = KeyPair.genKeyPair(JSCH, KeyPair.RSA);
        kpair.writePrivateKey(privateOut, passphrase == null ? null : getPassphrase(passphrase));
        kpair.writePublicKey(publicOut, comment == null ? DEFAULT_COMMENT : comment);
    }

    private static byte[] getPassphrase(String passphrase) throws IOException {
        if (passphrase == null) return null;
        File f = new File(passphrase);
        if (f.exists()) {
            return FileUtils.readFileToString(f).getBytes(StringUtil.UTF8cs);
        }
        return passphrase.getBytes(StringUtil.UTF8cs);
    }

    public boolean scpTo(File local, String remotePath, long timeout) throws Exception {
        @Cleanup("disconnect") final Channel channel = open("exec", (int) timeout);
        ((ChannelExec)channel).setCommand("scp -t "+remotePath);

        @Cleanup OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();
        checkAck(in);

        // send "C0644 filesize filename", where filename should not include '/'
        final String localPath = abs(local);
        long filesize = local.length();
        String command = "C0644 "+filesize+" ";
        if (localPath.lastIndexOf('/') > 0){
            command += localPath.substring(localPath.lastIndexOf('/')+1);
        } else {
            command += localPath;
        }
        command += "\n";
        out.write(command.getBytes(UTF8)); out.flush();
        checkAck(in);

        // send a content of local file
        try (InputStream fis = new FileInputStream(local)) {
            IOUtils.copy(fis, out);
        }

        // send '\0'
        out.write(NULL_TERMINATOR, 0, 1);
        out.flush();
        checkAck(in);
        out.close();

        return true;
    }

    public String exec(String command, long timeout) throws Exception {

        @Cleanup("disconnect") Channel channel = open("exec", (int) timeout);
        ((ChannelExec)channel).setCommand(command);

        channel.setInputStream(null);

        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        ((ChannelExec)channel).setErrStream(err);

        InputStream in = channel.getInputStream();
        channel.connect();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);

        final long start = now();
        while (!channel.isClosed() && !isTimedOut(start, timeout)) {
            Thread.sleep(100);
        }
        if (!channel.isClosed()) die("Output completed but channel still open, command="+command+", output="+out.toString(UTF8));

        int exitStatus = channel.getExitStatus();
        if (exitStatus != 0) die("Error executing command ("+command+"): exit status "+exitStatus+" (output="+out.toString(UTF8)+")");

        return out.toString(UTF8);
    }

    private boolean isTimedOut(long start, long timeout) { return now() - start > timeout; }

    private static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b==0) return b;
        if (b==-1) die("checkAckFailed ("+b+")");

        StringBuilder sb = new StringBuilder();
        int c;
        do {
            c = in.read();
            sb.append((char)c);
        } while (c != '\n');
        if (b==1){ // error
            die("checkAck failed ("+b+"): "+ sb.toString());
        }
        // fatal error
        return die("checkAck failed ("+b+"): "+ sb.toString());
    }

    public boolean ssh(long timeout) throws Exception {
        @Cleanup("disconnect") Channel channel = open("shell", (int) timeout);
        channel.setInputStream(System.in);
        if (SystemUtils.IS_OS_WINDOWS) {
            // a hack for MS-DOS prompt on Windows.
            channel.setInputStream(new FilterInputStream(System.in){
                public int read(byte[] b, int off, int len)throws IOException{
                    return in.read(b, off, (len>1024?1024:len));
                }
            });
        }

        channel.setOutputStream(System.out);

      /*
      // Choose the pty-type "vt102".
      ((ChannelShell)channel).setPtyType("vt102");
      */

      /*
      // Set environment variable "LANG" as "ja_JP.eucJP".
      ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
      */

        //channel.connect();
        channel.connect((int) timeout);

        while (!channel.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        return true;
    }
}
