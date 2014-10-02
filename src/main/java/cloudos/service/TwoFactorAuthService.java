package cloudos.service;

import com.authy.AuthyApiClient;
import com.authy.api.Hash;
import com.authy.api.Token;
import com.authy.api.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cobbzilla.util.http.ApiConnectionInfo;

@AllArgsConstructor
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

    public void deleteUser(Integer authId) {
        final Hash result = getClient().getUsers().deleteUser(authId);
        if (!result.isOk()) throw new IllegalStateException("Error deleting authy user: "+result.getError());
    }

}
