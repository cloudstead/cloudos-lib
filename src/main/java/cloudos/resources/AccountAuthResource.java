package cloudos.resources;

import cloudos.dao.AccountBaseDAO;
import cloudos.model.AccountBase;
import cloudos.model.auth.ResetPasswordRequest;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.wizard.resources.ResourceUtil;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AccountAuthResource<A extends AccountBase> {

    public static final String EP_ACTIVATE = "/activate";
    public static final String EP_FORGOT_PASSWORD = "/forgot_password";
    public static final String EP_RESET_PASSWORD = "/reset_password";

    public static final String PARAM_KEY = "key";
    public static final String PARAM_RESETPASSWORD_URL = "resetPasswordUrl";

    protected abstract AccountBaseDAO<A> getAccountBaseDAO();
    protected abstract TemplatedMailService getTemplatedMailService();

    protected abstract String getResetPasswordUrl(String token);

    protected long getVerificationCodeExpiration() { return TimeUnit.DAYS.toMillis(2); }

    @GET
    @Path(EP_ACTIVATE+"/{"+PARAM_KEY+"}")
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

        return Response.ok().build();
    }

    // allows subclasses to add params to the forgot-password email
    protected void addForgotPasswordParams(Map<String, Object> params) {}

    @POST
    @Path(EP_FORGOT_PASSWORD)
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
                .setTemplateName(TemplatedMailService.T_RESET_PASSWORD)
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

    @POST
    @Path(EP_RESET_PASSWORD)
    public Response resetPassword (ResetPasswordRequest request) {

        final AccountBaseDAO<A> accountBaseDAO = getAccountBaseDAO();
        final A found = accountBaseDAO.findByResetPasswordToken(request.getToken());

        if (found == null) return Response.ok().build();
        if (found.getHashedPassword().getResetTokenAge() > getVerificationCodeExpiration()) return ResourceUtil.invalid("err.key.expired");

        accountBaseDAO.setPassword(found, request.getPassword());

        return Response.ok().build();
    }

}
