package cloudos.cslib.ssh;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang.RandomStringUtils;

import java.io.ByteArrayOutputStream;

@Accessors(chain=true)
public class CsKeyPair {

    @Getter @Setter String privateKey;
    @Getter @Setter String publicKey;
    @Getter @Setter String passphrase;

    public static CsKeyPair createKeyPairWithoutPassphrase() throws Exception {
        return createKeyPair(null);
    }

    public static CsKeyPair createKeyPair() throws Exception {
        return createKeyPair(RandomStringUtils.randomAlphanumeric(20));
    }

    public static CsKeyPair createKeyPair(String passphrase) throws Exception {

        @Cleanup final ByteArrayOutputStream privateOut = new ByteArrayOutputStream();
        @Cleanup final ByteArrayOutputStream publicOut = new ByteArrayOutputStream();

        CsJsch.generateKeyPair(privateOut, publicOut, passphrase);

        return new CsKeyPair()
                .setPrivateKey(privateOut.toString())
                .setPublicKey(publicOut.toString())
                .setPassphrase(passphrase);
    }

}
