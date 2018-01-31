package cloudos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.filters.Scrubbable;
import org.cobbzilla.wizard.filters.ScrubbableField;
import org.cobbzilla.wizard.model.BasicAccount;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.Email;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.wizard.model.crypto.EncryptedTypes.*;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass @Accessors(chain=true)
public class AccountBase extends UniquelyNamedEntity implements Scrubbable, BasicAccount {

    public static final String EMAIL_VERIFICATION_CODE = "emailVerificationCode";
    public static final String RESET_PASSWORD_TOKEN = "resetToken";

    @JsonIgnore public int getVerifyCodeLength () { return 16; }

    private static final ScrubbableField[] SCRUBBABLE_FIELDS = new ScrubbableField[]{
            new ScrubbableField(AccountBase.class, "authId", String.class)
    };

    @Override @JsonIgnore public ScrubbableField[] fieldsToScrub() { return SCRUBBABLE_FIELDS; }

    public static final String ERR_AUTHID_LENGTH = "{err.authid.length}";
    public static final String ERR_EMAIL_INVALID = "{err.email.invalid}";
    public static final String ERR_EMAIL_EMPTY = "{err.email.empty}";
    public static final String ERR_EMAIL_LENGTH = "{err.email.length}";
    public static final String ERR_LAST_NAME_EMPTY = "{err.lastName.empty}";
    public static final String ERR_LAST_NAME_LENGTH = "{err.lastName.length}";
    public static final String ERR_FIRST_NAME_EMPTY = "{err.firstName.empty}";
    public static final String ERR_FIRST_NAME_LENGTH = "{err.firstName.length}";
    public static final String ERR_MOBILEPHONE_LENGTH = "{err.mobilePhone.length}";
    public static final String ERR_MOBILEPHONE_EMPTY = "{err.mobilePhone.empty}";
    public static final String ERR_MOBILEPHONE_CC_EMPTY = "{err.mobilePhoneCountryCode.empty}";
    public static final String ERR_LOCALE_LENGTH = "{err.locale.length}";
    public static final int EMAIL_MAXLEN = 200;
    public static final int VERIFY_CODE_MAXLEN = 100;
    public static final int LASTNAME_MAXLEN = 100;
    public static final int FIRSTNAME_MAXLEN = 100;
    public static final int MOBILEPHONE_MAXLEN = 30;
    public static final int MOBILEPHONE_MINLEN = 8;
    public static final int LOCALE_MAXLEN = 40;

    @Getter @Setter @Embedded
    @JsonIgnore private HashedPassword hashedPassword;

    @Override public String initResetToken() { return hashedPassword.initResetToken(); }
    @Override @JsonIgnore public long getResetTokenAge() { return hashedPassword == null ? Long.MAX_VALUE : hashedPassword.getResetTokenAge(); }
    @Override @JsonIgnore public String getResetToken() { return hashedPassword == null ? null : hashedPassword.getResetToken(); }
    @Override public void setResetToken(String token) { hashedPassword.setResetToken(token); }

    @Override public AccountBase setPassword(String newPassword) {
        if (hashedPassword == null) {
            hashedPassword = new HashedPassword(newPassword);
        } else {
            hashedPassword.setPassword(newPassword);
        }
        return this;
    }

    @Size(max=30, message=ERR_AUTHID_LENGTH) @Column(length=30)
    @Getter @Setter private String authId = null;

    public boolean hasAuthId() { return !empty(authId); }

    @JsonIgnore @Transient public Integer getAuthIdInt() { return safeInt(authId); }
    public AccountBase setAuthIdInt(int authId) { setAuthId(String.valueOf(authId)); return this; }

    @Transient
    public String getAccountName () { return getName(); }
    public AccountBase setAccountName (String name) { setName(name); return this; }

    @HasValue(message=ERR_LAST_NAME_EMPTY)
    @Size(max=LASTNAME_MAXLEN, message=ERR_LAST_NAME_LENGTH)
    @Column(columnDefinition="varchar("+(LASTNAME_MAXLEN+ENC_PAD)+") NOT NULL")
    @Type(type=ENCRYPTED_STRING) @Getter @Setter private String lastName;

    @HasValue(message=ERR_FIRST_NAME_EMPTY)
    @Size(max=FIRSTNAME_MAXLEN, message=ERR_FIRST_NAME_LENGTH)
    @Column(columnDefinition="varchar("+(FIRSTNAME_MAXLEN+ENC_PAD)+") NOT NULL")
    @Type(type=ENCRYPTED_STRING) @Getter @Setter private String firstName;

    @JsonIgnore public String getFullName() { return getFirstName() + " " + getLastName(); }
    @JsonIgnore public String getLastNameFirstName() { return getLastName() + ", " + getFirstName(); }

    @Getter @Setter private boolean admin = false;
    @Getter @Setter private boolean suspended = false;
    @Getter @Setter private boolean twoFactor = false;

    @Getter @Setter private Long lastLogin = null;
    public void setLastLogin () { lastLogin = now(); }

    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Column(columnDefinition="varchar("+EMAIL_MAXLEN+") UNIQUE NOT NULL")
    @JsonIgnore @Getter @Setter private String canonicalEmail;

    public static String canonicalizeEmail (String email) {
        return canonicalizeEmail(email, true);
    }

    public static String canonicalizeEmail(String email, boolean logInvalidEmail) {
        if (empty(email)) throw invalidEx(ERR_EMAIL_EMPTY);
        int atPos = email.indexOf('@');
        if (atPos == -1 || atPos == email.length()-1) {
            throw invalidEx(ERR_EMAIL_INVALID, "email was invalid", email, logInvalidEmail);
        }
        String addr = email.substring(0, atPos);
        String domain = email.substring(atPos+1);
        return (urlEncode(addr) + "@" + domain).toLowerCase();
    }

    @Email(message=ERR_EMAIL_INVALID)
    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Column(columnDefinition="varchar("+(EMAIL_MAXLEN+ENC_PAD)+")")
    @Getter @Type(type=ENCRYPTED_STRING) private String email;

    public AccountBase setEmail (String email) {
        if (this.email == null || !this.email.equals(email)) {
            emailVerified = false;
            emailVerificationCode = null;
            emailVerificationCodeCreatedAt = null;
            canonicalEmail = canonicalizeEmail(email);
            this.email = email;
        }
        return this;
    }

    @JsonIgnore @Size(max=VERIFY_CODE_MAXLEN) @Getter @Setter private String emailVerificationCode;
    @JsonIgnore @Getter @Setter private Long emailVerificationCodeCreatedAt;
    @Getter private boolean emailVerified = false;

    public String initEmailVerificationCode() {
        emailVerificationCode = randomAlphanumeric(getVerifyCodeLength());
        emailVerificationCodeCreatedAt = now();
        return emailVerificationCode;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
        emailVerificationCode = null;
        emailVerificationCodeCreatedAt = null;
    }

    public boolean isEmailVerificationCodeValid (long expiration) {
        return emailVerificationCodeCreatedAt != null && emailVerificationCodeCreatedAt > (now() - expiration);
    }

    @Size(min=MOBILEPHONE_MINLEN, max=MOBILEPHONE_MAXLEN+ENC_PAD, message=ERR_MOBILEPHONE_LENGTH)
    @HasValue(message=ERR_MOBILEPHONE_EMPTY)
    @Column(nullable=false, columnDefinition="varchar("+(MOBILEPHONE_MAXLEN+ENC_PAD)+") NOT NULL")
    @Getter @Type(type=ENCRYPTED_STRING) private String mobilePhone;
    public AccountBase setMobilePhone (String mobilePhone) {
        if (this.mobilePhone == null || !this.mobilePhone.equals(mobilePhone)) {
            this.authId = null;
            this.mobilePhone = mobilePhone;
        }
        return this;
    }

    @HasValue(message=ERR_MOBILEPHONE_CC_EMPTY)
    @Column(nullable=false, length=ENC_INT)
    @Getter @Type(type=ENCRYPTED_INTEGER) private Integer mobilePhoneCountryCode;

    public AccountBase setMobilePhoneCountryCode(Integer mobilePhoneCountryCode) {
        if (this.mobilePhoneCountryCode == null || !this.mobilePhoneCountryCode.equals(mobilePhoneCountryCode)) {
            this.authId = null;
            this.mobilePhoneCountryCode = mobilePhoneCountryCode;
        }
        return this;
    }

    @JsonIgnore @Transient public String getMobilePhoneCountryCodeString() { return mobilePhoneCountryCode == null ? null : mobilePhoneCountryCode.toString(); }

    @Size(max=LOCALE_MAXLEN, message=ERR_LOCALE_LENGTH)
    @Column(columnDefinition="varchar("+(LOCALE_MAXLEN+ENC_PAD)+")")
    @Getter @Setter @Type(type=ENCRYPTED_STRING) private String locale;
    @JsonIgnore public boolean hasLocale () { return !empty(locale); }

    public AccountBase populate(AccountBase other) {
        setName(other.getName());
        setEmail(other.getEmail());
        setFirstName(other.getFirstName());
        setLastName(other.getLastName());
        setMobilePhone(other.getMobilePhone());
        setMobilePhoneCountryCode(other.getMobilePhoneCountryCode());
        setAdmin(other.isAdmin());
        setSuspended(other.isSuspended());
        setTwoFactor(other.isTwoFactor());
        setAuthId(other.getAuthId());
        if (other.getLastLogin() != null) setLastLogin(other.getLastLogin());
        if (getHashedPassword() != null && other.getHashedPassword() != null) {
            getHashedPassword().setResetToken(other.getHashedPassword().getResetToken());
        }
        setLocale(other.getLocale());
        return this;
    }

    @NoArgsConstructor
    public static class PublicView {
        @Getter @Setter public String name;
        @Getter @Setter public String firstName;
        @Getter @Setter public String lastName;
        @Getter @Setter public String fullName;
        public PublicView (AccountBase other) { copy(this, other); }
    }
}
