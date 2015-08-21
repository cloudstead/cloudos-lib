package cloudos.cslib.compute.jclouds;

import cloudos.cslib.compute.CsCloudBase;
import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import cloudos.cslib.ssh.CsKeyPair;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Module;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.location.reference.LocationConstants;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.jclouds.compute.config.ComputeServiceProperties.SOCKET_FINDER_ALLOWED_INTERFACES;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;

@Slf4j
public class JcloudBase extends CsCloudBase {

    public static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex(Apis.viewableAs(ComputeServiceContext.class),
            Apis.idFunction());

    public static final Map<String, ProviderMetadata> apiProviders = Maps.uniqueIndex(Providers.viewableAs(ComputeServiceContext.class),
            Providers.idFunction());

    protected String getRegion() { return config.getRegion(); }
    protected String getImage() { return config.getImage(); }
    protected String getInstanceSize() { return config.getInstanceSize(); }

    @Override
    public CsInstance newInstance(CsInstanceRequest request) throws Exception {

        @Cleanup ComputeServiceContext context = initComputeService();
        ComputeService compute = context.getComputeService();

        TemplateBuilder templateBuilder = compute.templateBuilder();

        final String groupName = config.getGroupPrefix() + RandomStringUtils.randomAlphanumeric(10).toLowerCase()+"-"+System.currentTimeMillis();

        final CsKeyPair keyPair = CsKeyPair.createKeyPairWithoutPassphrase();
        final String adminPrivateKey = keyPair.getPrivateKey();
        final String adminPublicKey = keyPair.getPublicKey();
        final String adminPassword = RandomStringUtils.randomAlphanumeric(20);
        final String loginPassword = RandomStringUtils.randomAlphanumeric(20);

        final String user = config.getUser();
        Statement bootStatement = new AdminAccess.Builder()
                .adminUsername(user)
                .adminPublicKey(adminPublicKey)
                .adminPrivateKey(adminPrivateKey)
                .adminPassword(adminPassword)
                .loginPassword(loginPassword)
                .grantSudoToAdminUser(true)
                .lockSsh(true)
                .build();
        templateBuilder.options(runScript(bootStatement));

        Template template;
        try {
            template = templateBuilder
                    .hardwareId(getInstanceSize())
                    .imageId(getImage())
                    .locationId(getRegion())
                    .build();
        } catch (Exception e) {
            log.error("error creating template: "+e, e);
            throw e;
        }

        // build the actual instance
        final NodeMetadata node = getOnlyElement(compute.createNodesInGroup(groupName, 1, template));

        log.info(String.format("<< node %s: %s%n", node.getId(), concat(node.getPrivateAddresses(), node.getPublicAddresses())));

        final CsInstance instance = new CsInstance();
        instance.setCloudConfig(config);
        instance.setVendorId(node.getId());
        instance.setHost(request.getHost());
        instance.setPort(22);
        instance.setUser(user);
        instance.setKey(adminPrivateKey);
        instance.setPassphrase(keyPair.getPassphrase());
        instance.setGroup(groupName);
        instance.setPublicAddresses(node.getPublicAddresses());
        instance.setPrivateAddresses(node.getPrivateAddresses());

        return instance;
    }

    private ComputeServiceContext initComputeService() {

        // example of specific properties, in this case optimizing image list to
        // only amazon supplied
        Properties properties = new Properties();
//        properties.setProperty(PROPERTY_EC2_AMI_QUERY, "owner-id=137112412989;architecture=x86_64;state=available;image-type=machine");
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

    @Override
    public boolean teardown(CsInstance instance) throws Exception {
        @Cleanup ComputeServiceContext context = initComputeService();
        ComputeService compute = context.getComputeService();
        Set<? extends NodeMetadata> destroyed = compute.destroyNodesMatching(
                Predicates.<NodeMetadata> and(not(TERMINATED), inGroup(instance.getGroup())));
        log.info("Destroyed: "+destroyed);
        return !destroyed.isEmpty();
    }

}
