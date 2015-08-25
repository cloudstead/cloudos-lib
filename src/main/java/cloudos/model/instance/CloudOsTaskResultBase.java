package cloudos.model.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.task.TaskResult;
import org.cobbzilla.wizard.validation.SimpleViolationException;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@NoArgsConstructor @Slf4j
public class CloudOsTaskResultBase<A extends Identifiable, C extends CloudOsBase> extends TaskResult<CloudOsEvent> {

    @Getter @Setter protected A admin;
    @Getter @Setter protected C cloudOs;

    @JsonIgnore @Getter @Setter private DAO<CloudOsEvent> eventDAO;

    public CloudOsTaskResultBase(A admin, C cloudOs) {
        this.admin = admin;
        this.cloudOs = cloudOs;
    }

    public CloudOsTaskResultBase(A admin, C cloudOs, DAO<CloudOsEvent> eventDAO) {
        this.admin = admin;
        this.cloudOs = cloudOs;
        this.eventDAO = eventDAO;
    }

    @Override public void add(CloudOsEvent event) {
        super.add(event);
        eventDAO.create(event);
    }

    protected CloudOsEvent event(String messageKey) {
        return new CloudOsEvent(cloudOs.getUuid(), getTask(), messageKey);
    }

    public void error(String messageKey, String message, String invalidValue) {
        final SimpleViolationException e = new SimpleViolationException(messageKey, message, invalidValue);
        final CloudOsEvent event = event(messageKey);
        error(event, e);
    }

    public void error(String messageKey, String message) {
        log.error("ERROR: " + messageKey + " " + message);
        error(messageKey, message, null);
    }

    public void success(String messageKey) { success(event(messageKey)); }

    public void update(String messageKey) { add(event(messageKey)); }
}