package cloudos.resources;

import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.auth.ResetPasswordRequest;
import org.cobbzilla.wizard.dao.BasicAccountDAO;
import org.cobbzilla.wizard.model.BasicAccount;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.mail.service.TemplatedMailService.T_RESET_PASSWORD;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.trimQuotes;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Slf4j
public abstract class AuthResourceBase<A extends BasicAccount> {

    public static final String EP_ACTIVATE = "/activate";
    public static final String EP_FORGOT_PASSWORD = "/forgot_password";
    public static final String EP_RESET_PASSWORD = "/reset_password";

    public static final String PARAM_KEY = "key";
    public static final String PARAM_RESETPASSWORD_URL = "resetPasswordUrl";

    protected abstract BasicAccountDAO<A> getAccountDAO();
    protected abstract TemplatedMailService getTemplatedMailService();

    protected abstract String getResetPasswordUrl(String token);

    protected boolean addForgotPasswordParams(TemplatedMail mail, A account) {
        mail.setTemplateName(T_RESET_PASSWORD)
                .setFromName(getResetPasswordFromName(T_RESET_PASSWORD))
                .setFromEmail(getResetPasswordFromEmail(T_RESET_PASSWORD))
                .setParameter(PARAM_RESETPASSWORD_URL, getResetPasswordUrl(account.getResetToken()));
        return true;
    }

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

        final BasicAccountDAO<A> accountDAO = getAccountDAO();
        final A found = accountDAO.findByActivationKey(key);

        if (found == null) return notFound(key);

        if (found.getEmailVerificationCode().equals(key)) {
            if (found.isEmailVerificationCodeValid(getVerificationCodeExpiration())) {
                found.setEmailVerified(true);
                accountDAO.update(found);
            } else {
                return invalid("err.key.expired");
            }
        }

        if (!empty(getActivationSuccessRedirect())) {
            Response.temporaryRedirect(URI.create(getActivationSuccessRedirect()));
        }
        return ok();
    }

    protected String getFromName(String templateName) { return System.getProperty("user.name") + "@" + CommandShell.hostname(); }
    protected String getFromEmail(String templateName) { return getFromName(templateName); }

    // allows subclasses to customize to the forgot-password email
    protected String getResetPasswordFromName(String name) { return getFromName(name); }
    protected String getResetPasswordFromEmail(String name) { return getFromEmail(name); }

    public A findAccountForForgotPassword(String name) { return getAccountDAO().findByName(name); }

    /**
     * Forgot password: Send a reset password email
     * @param name The account name
     * @return Just an HTTP status code
     * @statuscode 403 If account with such email was not found.
     * @statuscode 200 Otherwise.
     */
    @POST
    @Path(EP_FORGOT_PASSWORD)
    @ReturnType("java.lang.Void")
    public Response forgotPassword (String name) {

        name = trimQuotes(name);
        if (empty(name)) return notFound();

        final BasicAccountDAO<A> accountBaseDAO = getAccountDAO();
        final A found = findAccountForForgotPassword(name);

        if (found == null) return forbidden();

        // generate a reset token and save it
        found.initResetToken();
        accountBaseDAO.update(found);

        final TemplatedMail mail = new TemplatedMail();
        if (!addForgotPasswordParams(mail, found)) return notFound();
        try {
            getTemplatedMailService().getMailSender().deliverMessage(mail);
        } catch (Exception e) {
            log.error("forgotPassword: Error sending email: "+e, e);
            return ok_empty();
        }

        return ok_empty();
    }

    /**
     * Reset a password.
     * @param request The reset password request
     * @return Just an HTTP status
     * @statuscode 403 If the key is invalid
     * @statuscode 422 If the key was valid, but has expired
     * @statuscode 200 If the key was not valid, or if the password was successfully reset.
     */
    @POST
    @Path(EP_RESET_PASSWORD)
    @ReturnType("java.lang.Void")
    public Response resetPassword (ResetPasswordRequest request) {

        final BasicAccountDAO<A> accountDAO = getAccountDAO();
        final A found = accountDAO.findByResetPasswordToken(request.getToken());

        if (found == null) return forbidden();
        if (found.getResetTokenAge() > getVerificationCodeExpiration()) return invalid("err.key.expired");

        found.setEmailVerified(true); // if you can reset a password, you must have been able to check your email
        found.setResetToken(null);
        accountDAO.setPassword(found, request.getPassword());

        return ok_empty();
    }

}
