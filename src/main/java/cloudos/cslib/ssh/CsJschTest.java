package cloudos.cslib.ssh;

import lombok.Cleanup;
import org.cobbzilla.util.io.FileUtil;

public class CsJschTest {

    public static final int TIMEOUT = 10000;

    public static void main (String[] args) throws Exception {

        final String privateKey = FileUtil.toString(System.getProperty("user.home")+"/.ssh/id_dsa");
        final CsJschConnectionInfo identity = new CsJschConnectionInfo()
                .setUser("jonathan")
                .setHost("kyuss.org")
                .setKey(privateKey);
        @Cleanup final CsJsch jsch = new CsJsch(identity);
        final String result = jsch.exec("echo 'we made it'", TIMEOUT);
        System.out.println("got result="+result);
    }
}
