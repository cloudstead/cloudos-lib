package cloudos.model.instance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.task.TaskBase;
import org.cobbzilla.wizard.task.TaskEvent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class CloudOsEvent extends TaskEvent {

    @Column(length=UUID_MAXLEN, nullable=false)
    @Getter @Setter private String cloudOsUuid;

    public CloudOsEvent(String cloudOsUuid, TaskBase task, String messageKey) {
        super(task, messageKey);
        this.cloudOsUuid = cloudOsUuid;
    }

}
