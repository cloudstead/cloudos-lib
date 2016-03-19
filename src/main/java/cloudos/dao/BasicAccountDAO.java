package cloudos.dao;

import cloudos.model.BasicAccount;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import org.cobbzilla.wizard.dao.DAO;

public interface BasicAccountDAO<A extends BasicAccount> extends DAO<A> {

    public A findByName(String name);

    public A authenticate(LoginRequest login) throws AuthenticationException;

    public A findByActivationKey(String key);

    public A findByResetPasswordToken(String token);

    public void setPassword(A account, String password);
}
