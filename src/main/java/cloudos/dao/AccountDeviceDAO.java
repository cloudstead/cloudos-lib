package cloudos.dao;

import cloudos.model.auth.AccountDevice;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDeviceDAO extends AbstractCRUDDAO<AccountDevice> {

    public AccountDevice findByAccountAndDevice(String account, String deviceId) {
        return uniqueResult(criteria().add(
                Restrictions.and(
                        Restrictions.eq("account", account),
                        Restrictions.eq("deviceId", deviceId)
                )
        ));
    }

}
