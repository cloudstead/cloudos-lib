package cloudos.dao;

import cloudos.model.BasicAccount;
import org.cobbzilla.wizard.dao.DAO;

public interface AccountBaseDAO<A extends BasicAccount> extends DAO<A> {

    public A findByActivationKey(String key);

    public A findByName(String name);

    public A findByResetPasswordToken(String token);

    public void setPassword(A account, String password);

}
