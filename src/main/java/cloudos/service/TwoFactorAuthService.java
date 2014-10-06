package cloudos.service;

import com.authy.AuthyApiClient;
import com.authy.api.Hash;
import com.authy.api.Token;
import com.authy.api.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.ApiConnectionInfo;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor @Slf4j
public class TwoFactorAuthService {

    final ApiConnectionInfo authy;

    @Getter(lazy=true) private final AuthyApiClient client = initAuthyApiClient();
    private AuthyApiClient initAuthyApiClient() {
        final String uri = authy.getBaseUri();
        return new AuthyApiClient(authy.getUser(), uri, uri.startsWith("http://"));
    }

    public int addUser(String email, String phone, String countrycode) {
        final User user = getClient().getUsers().createUser(email, phone, countrycode);
        if (!user.isOk()) throw new IllegalStateException("Error creating authy user: "+user.getError());
        return user.getId();
    }

    public void verify(int authId, String userToken) {
        final Token token = getClient().getTokens().verify(authId, userToken);
        if (!token.isOk()) throw new IllegalStateException("Error verifying authy user: "+token.getError());
    }

    public void sendSms (int authId) {
        Map<String, String> options = new HashMap<>();
        options.put("force", "true");
        final Hash hash = getClient().getUsers().requestSms(authId, options);
        // todo: fix authy-java library so that isOk returns true here (it should but doesn't)
        if (!hash.isSuccess()) throw new IllegalStateException("Error sending SMS token: "+hash.getError());
    }

    public void deleteUser(Integer authId) {
        if (authId == null) {
            log.warn("deleteUser: deleting 'null' is a noop, silently ignoring...");
            return;
        }
        final Hash result = getClient().getUsers().deleteUser(authId);
        if (!result.isOk()) log.warn("Error deleting authy user: '" + result.getError()+"'");
    }

}
