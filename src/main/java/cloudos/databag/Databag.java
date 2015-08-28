package cloudos.databag;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.toFileOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public abstract class Databag {

    public abstract String getId();

    public static File getChefFile(Class<? extends Databag> databag, File dir, String app) {
        return bag(databag).getDatabagFile(dir, app);
    }

    public static <T extends Databag> T fromChefRepo(Class<? extends Databag> databag, File dir, String app) {
        return bag(databag).fromChef(dir, app);
    }

    public static <T extends Databag> T fromChefRepoOrNew(Class<? extends Databag> bagClass, File dir, String app) {
        final Databag databag = bag(bagClass);
        final File databagFile = databag.getDatabagFile(dir, app);
        return (T) (empty(databagFile) ? instantiate(bagClass) : databag.fromChef(dir, app));
    }

    public <T extends Databag> T fromChef(File dir, String app) {
        final File databag = getDatabagFile(dir, app);
        if (!databag.exists()) die("fromChefRepo: databag not found: "+abs(databag));
        return (T) fromJsonOrDie(databag, getClass());
    }

    public File getDatabagFile(File dir, String app) { return new File(abs(dir) + "/data_bags/"+app+"/"+getId()+".json"); }

    public void toChefRepo(File dir, String app) {
        final File databagFile = getDatabagFile(dir, app);
        final File databagDir = databagFile.getParentFile();
        if (!databagDir.exists() && !databagDir.mkdirs()) {
            die("write: Error creating parent directory: "+ databagDir);
        }
        toFileOrDie(databagFile, toJsonOrDie(this));
    }

    private static Map<Class<? extends Databag>, Databag> cache = new ConcurrentHashMap<>();

    protected static Databag bag(Class<? extends Databag> bagClass) {
        Databag bag = cache.get(bagClass);
        if (bag == null) {
            bag = instantiate(bagClass);
            cache.put(bagClass, bag);
        }
        return bag;
    }
}
