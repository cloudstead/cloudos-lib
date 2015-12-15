package cloudos.model.instance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum CloudOsAppBundle {

    required    (Constants.REQUIRED_APPS, null),
    basic       (Constants.BASIC_APPS, required),
    small_biz   (Constants.SMALL_BIZ_APPS, basic),
    tech_startup(Constants.TECH_STARTUP_APPS, small_biz);

    private String[] apps;
    @JsonIgnore private CloudOsAppBundle parent;

    CloudOsAppBundle(String[] apps, CloudOsAppBundle parent) {
        this.apps = apps;
        this.parent = parent;
    }

    @JsonCreator public static CloudOsAppBundle create (String name) { return valueOf(name.toLowerCase()); }

    public List<String> getApps () {
        final List<String> appList = new ArrayList<>();
        if (parent != null) appList.addAll(parent.getApps());
        appList.addAll(Arrays.asList(apps));
        return appList;
    }

    // always return a copy to avoid mutating the enum
    @JsonIgnore public String[] getAppsArray () {
        final List<String> appList = getApps();
        return appList.toArray(new String[appList.size()]);
    }

    public static boolean isValid(String val) {
        try { return create(val) != null; } catch (Exception e) { return false; }
    }

    private static class Constants {

        public static final String[] REQUIRED_APPS = {
                "base", "apache", "postgresql", "mysql",
                "java", "git", "kolab", "kestrel", "cloudos"
        };

        public static final String[] BASIC_APPS = {
                "roundcube", "roundcube-calendar", "roundcube-addressbook",
                "owncloud"
        };

        public static final String[] SMALL_BIZ_APPS = {
                "limesurvey", "wordpress", "etherpad", "phplist", "kandan",
                "piwik", "dokuwiki"
        };

        public static final String[] TECH_STARTUP_APPS = {
                "gitlab", "kanban", "phabricator", "jira"
        };
    }
}
