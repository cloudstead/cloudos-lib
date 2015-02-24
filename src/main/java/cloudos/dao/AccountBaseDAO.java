package cloudos.dao;

import cloudos.model.AccountBase;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;

import java.util.List;

public abstract class AccountBaseDAO<T extends AccountBase> extends UniquelyNamedEntityDAO<T>  {

    public abstract T authenticate(LoginRequest loginRequest) throws AuthenticationException;

    public T findByActivationKey(String key) { return findByUniqueField("emailVerificationCode", key); }

    public T findByResetPasswordToken(String key) { return findByUniqueField("hashedPassword.resetToken", key); }

    public void setPassword(T account, String newPassword) {
        account.getHashedPassword().setPassword(newPassword);
        account.getHashedPassword().setResetToken(null);
        update(account);
    }

    public List<T> findAdmins() { return findByField("admin", true); }
}
