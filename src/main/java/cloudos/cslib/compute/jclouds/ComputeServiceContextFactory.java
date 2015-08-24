package cloudos.cslib.compute.jclouds;

import cloudos.cslib.compute.CsCloudConfig;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.CloseOnExit;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.location.reference.LocationConstants;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.jclouds.compute.config.ComputeServiceProperties.SOCKET_FINDER_ALLOWED_INTERFACES;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;

@Slf4j
public class ComputeServiceContextFactory {

    private Map<CsCloudConfig, ComputeServiceContext> cache = new ConcurrentHashMap<>();

    public ComputeServiceContext build(CsCloudConfig config) {
        ComputeServiceContext ctx = cache.get(config);
        if (ctx == null) {
            ctx = build_internal(config);
            CloseOnExit.add(ctx);
            cache.put(config, ctx);
        }
        return ctx;
    }

    protected ComputeServiceContext build_internal (CsCloudConfig config) {
        // example of specific properties, in this case optimizing image list to
        // only amazon supplied
        Properties properties = new Properties();
//        properties.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "owner-id=137112412989;architecture=x86_64;state=available;image-type=machine");
//        properties.setProperty(PROPERTY_EC2_CC_AMI_QUERY, "");
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, String.valueOf(scriptTimeout));
        properties.setProperty(SOCKET_FINDER_ALLOWED_INTERFACES, "PUBLIC");
        properties.setProperty(LocationConstants.PROPERTY_ZONES, config.getRegion());

        // example of injecting a ssh implementation
        Iterable<Module> modules = ImmutableSet.<Module> of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule(),
                new EnterpriseConfigurationModule());

        ContextBuilder builder = ContextBuilder.newBuilder(config.getType().getProviderName())
                .credentials(config.getAccountId(), config.getAccountSecret())
                .modules(modules)
                .overrides(properties);

        log.info(">> initializing " + builder.getApiMetadata());

        ComputeServiceContext context = builder.buildView(ComputeServiceContext.class);
        return context;
    }
}
