package cloudos.model.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.validation.SimpleViolationException;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@NoArgsConstructor @Slf4j
public class CloudOsStatusBase<A extends Identifiable, C extends CloudOsBase> {

    @Getter @Setter private A admin;
    @Getter @Setter private C cloudOs;
    @JsonIgnore @Getter private DAO<CloudOsEvent> eventDAO;

    @Getter @Setter private List<CloudOsEvent> history = new ArrayList<>();

    @Getter private SimpleViolationException exception;

    public void initRetry() {
        exception = null;
    }

    public boolean hasError() {
        return exception != null || (!history.isEmpty() && getMostRecentEvent().getMessageKey().contains(".error."));
    }

    public String getErrorMessageKey() {
        return hasError() && !history.isEmpty() ? getMostRecentEvent().getMessageKey() : null;
    }

    public void setErrorMessageKey(String ignored) {
    }  // so json won't complain

    public CloudOsStatusBase(A admin, C cloudOs) {
        this.admin = admin;
        this.cloudOs = cloudOs;
    }

    public CloudOsStatusBase(A admin, C cloudOs, DAO<CloudOsEvent> eventDAO) {
        this.admin = admin;
        this.cloudOs = cloudOs;
        this.eventDAO = eventDAO;
    }

    public void update(String statusMessageKey) {
        final CloudOsEvent event = new CloudOsEvent().setCloudOsUuid(cloudOs.getUuid()).setMessageKey(statusMessageKey);
        eventDAO.create(event);
        this.history.add(event);
    }

    public void success(String statusMessageKey) {
        update(statusMessageKey);
    }

    public void error(String messageKey, String message, String invalidValue) {
        exception = new SimpleViolationException(messageKey, message, invalidValue);
        update(messageKey);
    }

    public void error(String messageKey, String message) {
        log.error("ERROR: " + messageKey + " " + message);
        error(messageKey, message, null);
    }

    @JsonIgnore
    public boolean isCompleted() {
        final CloudOsEvent event = getMostRecentEvent();
        return event != null && (event.isCompleted() || event.isSuccess() || event.isError());
    }

    @JsonIgnore
    public boolean isSuccess() {
        final CloudOsEvent event = getMostRecentEvent();
        return event != null && event.isSuccess();
    }

    @JsonIgnore
    public boolean isError() {
        final CloudOsEvent event = getMostRecentEvent();
        return event != null && event.isError();
    }

    @JsonIgnore
    public CloudOsEvent getMostRecentEvent() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    public void completed() { update("{setup.completed}"); }

}