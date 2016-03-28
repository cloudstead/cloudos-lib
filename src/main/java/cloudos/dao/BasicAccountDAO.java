package cloudos.dao;

import cloudos.model.BasicAccount;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import org.cobbzilla.wizard.dao.DAO;

public interface BasicAccountDAO<A extends BasicAccount> extends DAO<A> {

    A findByName(String name);

    A authenticate(LoginRequest login) throws AuthenticationException;

    A findByActivationKey(String key);

    A findByResetPasswordToken(String token);

    void setPassword(A account, String password);
}
