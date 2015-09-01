package cloudos.databag;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum EmailServerType {

    custom (null, null),
    sendgrid ("smtp.sendgrid.net", 587),
    mailgun ("smtp.mailgun.org", 587),;

    @Getter private final String host;
    @Getter private final Integer port;

    @JsonCreator public static EmailServerType create (String val) { return valueOf(val.toLowerCase()); }

}
