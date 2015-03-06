package cloudos.resources;

import cloudos.dao.AccountBaseDAO;
import cloudos.model.AccountBase;
import cloudos.model.auth.ResetPasswordRequest;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.resources.ResourceUtil;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.mail.service.TemplatedMailService.T_RESET_PASSWORD;
import static org.cobbzilla.util.string.StringUtil.empty;

@Slf4j
public abstract class AuthResourceBase<A extends AccountBase> {

    public static final String EP_ACTIVATE = "/activate";
    public static final String EP_FORGOT_PASSWORD = "/forgot_password";
    public static final String EP_RESET_PASSWORD = "/reset_password";

    public static final String PARAM_KEY = "key";
    public static final String PARAM_RESETPASSWORD_URL = "resetPasswordUrl";

    protected abstract AccountBaseDAO<A> getAccountBaseDAO();
    protected abstract TemplatedMailService getTemplatedMailService();

    protected abstract String getResetPasswordUrl(String token);

    protected long getVerificationCodeExpiration() { return TimeUnit.DAYS.toMillis(2); }

    protected String getActivationSuccessRedirect() { return null; }

    /**
     * Activate a user account
     * @param key The activation key (from email)
     * @return Just an HTTP status code
     * @statuscode 404 activation key not found
     * @statuscode 200 activation successful
     */
    @GET
    @Path(EP_ACTIVATE+"/{"+PARAM_KEY+"}")
    @ReturnType("java.lang.Void")
    public Response activate (@PathParam(PARAM_KEY) String key) {

        final AccountBaseDAO<A> accountBaseDAO = getAccountBaseDAO();
        final A found = accountBaseDAO.findByActivationKey(key);

        if (found == null) return ResourceUtil.notFound(key);

        if (found.getEmailVerificationCode().equals(key)) {
            if (found.isEmailVerificationCodeValid(getVerificationCodeExpiration())) {
                found.setEmailVerified(true);
                accountBaseDAO.update(found);
            } else {
                return ResourceUtil.invalid("err.key.expired");
            }
        }

        if (!empty(getActivationSuccessRedirect())) {
            Response.temporaryRedirect(URI.create(getActivationSuccessRedirect()));
        }
        return Response.ok().build();
    }

    protected String getFromName(String templateName) { return System.getProperty("user.name") + "@" + CommandShell.hostname(); }
    protected String getFromEmail(String templateName) { return getFromName(templateName); }

    // allows subclasses to add params to the forgot-password email
    protected void addForgotPasswordParams(Map<String, Object> params) {}

    /**
     * Forgot password: Send a reset password email
     * @param name The account name
     * @return Just an HTTP status code
     * @statuscode 200 Regardless.
     */
    @POST
    @Path(EP_FORGOT_PASSWORD)
    @ReturnType("java.lang.Void")
    public Response forgotPassword (String name) {

        final AccountBaseDAO<A> accountBaseDAO = getAccountBaseDAO();
        final A found = accountBaseDAO.findByName(name);

        if (found == null) return Response.ok().build();

        // generate a reset token
        final String token = found.getHashedPassword().initResetToken();
        accountBaseDAO.update(found);

        final TemplatedMail mail = new TemplatedMail()
                .setToEmail(found.getEmail())
                .setToName(found.getFullName())
                .setFromName(getFromName(T_RESET_PASSWORD))
                .setFromEmail(getFromEmail(T_RESET_PASSWORD))
                .setTemplateName(T_RESET_PASSWORD)
                .setParameter(TemplatedMailService.PARAM_ACCOUNT, found)
                .setParameter(PARAM_RESETPASSWORD_URL, getResetPasswordUrl(token));
        addForgotPasswordParams(mail.getParameters());
        try {
            getTemplatedMailService().getMailSender().deliverMessage(mail);
        } catch (Exception e) {
            log.error("forgotPassword: Error sending email: "+e, e);
            return Response.ok().build();
        }

        return Response.ok().build();
    }

    /**
     * Reset a password.
     * @param request The reset password request
     * @return Just an HTTP status
     * @statuscode 422 If the key was valid, but has expired
     * @statuscode 200 If the key was not valid, or if the password was successfully reset.
     */
    @POST
    @Path(EP_RESET_PASSWORD)
    @ReturnType("java.lang.Void")
    public Response resetPassword (ResetPasswordRequest request) {

        final AccountBaseDAO<A> accountBaseDAO = getAccountBaseDAO();
        final A found = accountBaseDAO.findByResetPasswordToken(request.getToken());

        if (found == null) return Response.ok().build();
        if (found.getHashedPassword().getResetTokenAge() > getVerificationCodeExpiration()) return ResourceUtil.invalid("err.key.expired");

        accountBaseDAO.setPassword(found, request.getPassword());

        return Response.ok().build();
    }

}
