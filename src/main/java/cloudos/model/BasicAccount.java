package cloudos.model;

import org.cobbzilla.wizard.model.Identifiable;

public interface BasicAccount extends Identifiable {

    public Integer getAuthIdInt();

    public String getName();

    public boolean isTwoFactor();

    public boolean isSuspended();

    public void setLastLogin();

    public String getEmailVerificationCode();

    public boolean isEmailVerificationCodeValid(long expiration);

    public void setEmailVerified(boolean verified);

    public String initResetToken();

    public String getEmail();

    public String getFullName();

    public long getResetTokenAge();

    public BasicAccount setPassword(String newPassword);

    public void setResetToken(String token);
}
