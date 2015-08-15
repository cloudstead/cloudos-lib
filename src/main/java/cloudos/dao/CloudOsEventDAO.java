package cloudos.dao;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.stereotype.Repository;
import cloudos.model.instance.CloudOsEvent;

import java.util.List;

@Repository
public class CloudOsEventDAO extends AbstractCRUDDAO<CloudOsEvent> {

    public List<CloudOsEvent> findByCloudOs(String uuid) {
        return findByField("cloudOsUuid", uuid);
    }

}
