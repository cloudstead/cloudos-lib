package cloudos.dao;

import cloudos.model.AccountBase;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;

public abstract class AccountBaseDAOBase<A extends AccountBase>
        extends AbstractCRUDDAO<A>
        implements AccountBaseDAO<A>, BasicAccountDAO<A> {

    @Override public A findByActivationKey(String key) {
        return findByUniqueField(AccountBase.EMAIL_VERIFICATION_CODE, key);
    }

    @Override public A findByName(String name) { return findByUniqueField("name", name); }

    @Override public A findByResetPasswordToken(String token) {
        return findByUniqueField(AccountBase.RESET_PASSWORD_TOKEN, token);
    }

    public void setPassword(A account, String newPassword) {
        account.setResetToken(null);
        account.setPassword(newPassword);
        update(account);
    }
}
