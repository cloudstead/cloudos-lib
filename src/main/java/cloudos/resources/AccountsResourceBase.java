package cloudos.resources;

import cloudos.dao.AccountDeviceDAO;
import cloudos.dao.BasicAccountDAO;
import cloudos.model.BasicAccount;
import cloudos.model.auth.AccountDevice;
import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import cloudos.server.HasTwoFactorAuthConfiguration;
import cloudos.service.TwoFactorAuthService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public abstract class AccountsResourceBase<A extends BasicAccount, R extends AuthResponse> {

    protected void beforeSessionStart(LoginRequest login, A account) throws Exception {}
    protected abstract void afterSuccessfulLogin(LoginRequest login, A account) throws Exception;
    protected abstract R buildAuthResponse(String sessionId, A account);

    @Autowired protected BasicAccountDAO<A> accountBaseDAO;
    @Autowired protected HasTwoFactorAuthConfiguration twoFactorConfig;
    @Autowired protected AbstractSessionDAO<A> sessionDAO;
    @Autowired protected AccountDeviceDAO deviceDAO;

    protected TwoFactorAuthService getTwoFactorAuthService() { return twoFactorConfig.getTwoFactorAuthService(); }

    /**
     * Login. The login request object can take two forms, one for initial login and the second for 2-factor authentication.
     * @see <a href="../api-examples/index.html">some API examples illustrating proper usage</a>
     * @param login The login request.
     * @return An AuthResponse object containing the session ID and the account that logged in
     * @statuscode 404 If the username, password, or 2-factor token was incorrect
     * @statuscode 403 If the user is suspended
     */
    @POST
    @ReturnType("R")
    public Response login(@Valid LoginRequest login) {

        final R authResponse;
        long start = System.currentTimeMillis();
        try {
            final A account;
            if (login.isSecondFactor()) {
                account = accountBaseDAO.findByName(login.getName().toLowerCase());
                if (account == null) return ResourceUtil.notFound();
                try {
                    getTwoFactorAuthService().verify(account.getAuthIdInt(), login.getSecondFactor());
                } catch (Exception e) {
                    return ResourceUtil.notFound();
                }

            } else {
                try {
                    account = accountBaseDAO.authenticate(login);
                } catch (AuthenticationException e) {
                    log.warn("Error authenticating: " + e);
                    switch (e.getProblem()) {
                        case NOT_FOUND:
                            return ResourceUtil.notFound();
                        case INVALID:
                            return ResourceUtil.notFound();
                        case BOOTCONFIG_ERROR:
                        default:
                            return Response.serverError().build();
                    }
                }

                // check for 2-factor
                if (account.isTwoFactor()) {
                    // if a device was supplied, check to see that most recent auth-time for that device
                    if (!deviceIsAuthorized(account, login.getDeviceId())) {
                        return Response.ok(buildAuthResponse(AuthResponse.TWO_FACTOR_SID, null)).build();
                    }
                }
            }

            // authenticate above should have returned 403 when the password didn't match, since
            // when an account is suspended its LDAP password is changed to a long random string.
            // ...but just in case...
            if (account.isSuspended()) return ResourceUtil.forbidden();

            // update last login
            account.setLastLogin();
            accountBaseDAO.update(account);

            // if this was a 2-factor success and a deviceId was given, update the device auth time
            if (login.isSecondFactor() && login.hasDevice()) {
                updateDeviceAuth(account, login.getDeviceId(), login.getDeviceName());
            }

            beforeSessionStart(login, account);
            final String sessionId = sessionDAO.create(account);
            afterSuccessfulLogin(login, account);

            authResponse = buildAuthResponse(sessionId, account);

        } catch (Exception e) {
            log.error("Error logging in account: "+e, e);
            return Response.serverError().build();

        } finally {
            log.info("login executed in "+ TimeUtil.formatDurationFrom(start));
        }

        return Response.ok(authResponse).build();
    }

    public static final long DEVICE_TIMEOUT = TimeUnit.DAYS.toMillis(30);

    private boolean deviceIsAuthorized(A account, String deviceId) {
        if (empty(deviceId)) return false;
        final AccountDevice accountDevice = deviceDAO.findByAccountAndDevice(account.getName(), deviceId);
        return accountDevice != null && accountDevice.isAuthYoungerThan(DEVICE_TIMEOUT);
    }

    protected void updateDeviceAuth(A account, String deviceId, String deviceName) {
        if (empty(deviceId)) return;
        final AccountDevice accountDevice = deviceDAO.findByAccountAndDevice(account.getName(), deviceId);
        if (accountDevice == null) {
            deviceDAO.create(new AccountDevice()
                    .setAccount(account.getName())
                    .setDeviceId(deviceId)
                    .setDeviceName(deviceName)
                    .setAuthTime());
        } else {
            deviceDAO.update(accountDevice.setDeviceName(deviceName).setAuthTime());
        }
    }
}
