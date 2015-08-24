package cloudos.cslib.compute.jclouds;

import cloudos.cslib.compute.CsCloudBase;
import cloudos.cslib.compute.instance.CsInstance;
import cloudos.cslib.compute.instance.CsInstanceRequest;
import cloudos.cslib.ssh.CsKeyPair;
import com.google.common.collect.Maps;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;

@Slf4j
public class JcloudBase extends CsCloudBase {

    public static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex(Apis.viewableAs(ComputeServiceContext.class),
            Apis.idFunction());

    public static final Map<String, ProviderMetadata> apiProviders = Maps.uniqueIndex(Providers.viewableAs(ComputeServiceContext.class),
            Providers.idFunction());
    public static final ComputeServiceContextFactory CONTEXT_FACTORY = new ComputeServiceContextFactory();

    protected String getRegion() { return config.getRegion(); }
    protected String getImage() { return config.getImage(); }
    protected String getInstanceSize() { return config.getInstanceSize(); }

    @Override
    public CsInstance newInstance(CsInstanceRequest request) throws Exception {

        @Cleanup final ComputeServiceContext context = getComputeService();
        final ComputeService compute = context.getComputeService();

        final TemplateBuilder templateBuilder = compute.templateBuilder();

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

        final Template template;
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
        final NodeMetadata node;
        try {
            node = getOnlyElement(compute.createNodesInGroup(groupName, 1, template));
        } catch (Exception e) {
            log.error("error starting instance: "+e, e);
            throw e;
        }

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

    private ComputeServiceContext getComputeService() {
        return CONTEXT_FACTORY.build(config);
    }

    @Override public boolean isRunning(CsInstance instance) throws Exception {
        final ComputeServiceContext context = getComputeService();
        final ComputeService compute = context.getComputeService();
        final Set<? extends ComputeMetadata> nodes = compute.listNodes();
        if (nodes.isEmpty()) return false;
        for (ComputeMetadata metadata : nodes) {
            if (metadata instanceof NodeMetadata) {
                final NodeMetadata node = (NodeMetadata) metadata;
                if (node.getGroup().startsWith(config.getGroupPrefix()) && node.getStatus() != NodeMetadata.Status.TERMINATED) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override public int teardown(CsInstance instance) throws Exception {
        int destroyed = 0;
        final ComputeServiceContext context = getComputeService();
        final ComputeService compute = context.getComputeService();
        final Set<? extends ComputeMetadata> nodes = compute.listNodes();
        for (ComputeMetadata metadata : nodes) {
            if (metadata instanceof NodeMetadata) {
                final NodeMetadata node = (NodeMetadata) metadata;
                if (node.getGroup().startsWith(config.getGroupPrefix()) && node.getStatus() != NodeMetadata.Status.TERMINATED) {
                    try {
                        compute.destroyNode(node.getId());
                        destroyed++;
                    } catch (Exception e) {
                        log.error("teardown: "+e, e);
                    }
                }
            }
        }
        return destroyed;
    }

}
