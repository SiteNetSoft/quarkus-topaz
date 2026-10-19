package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.AUTHORIZER_PORT;
import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.CONSOLE_PORT;
import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.DIRECTORY_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.sitenetsoft.quarkus.topaz.deployment.TopazBuildTimeConfig;
import org.testcontainers.utility.DockerImageName;

import aserto.directory.importer.v3.ImportRequest;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Vertx;

/**
 * Starts a Topaz container in dev and test mode, or reuses a shared or Compose one, and points the client at it.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesTopazProcessor {

    private static final Logger LOG = Logger.getLogger(DevServicesTopazProcessor.class);

    static final String FEATURE = "topaz";
    static final String AUTHORIZER_HOST = "quarkus.topaz.authorizer.host";
    static final String AUTHORIZER_PORT_KEY = "quarkus.topaz.authorizer.port";
    static final String DIRECTORY_HOST = "quarkus.topaz.directory.host";
    static final String DIRECTORY_PORT_KEY = "quarkus.topaz.directory.port";
    static final String PLAIN_TEXT = "quarkus.topaz.plain-text";
    static final String API_KEY = "quarkus.topaz.api-key";
    static final String CONSOLE_URL = "quarkus.topaz.console-url";

    private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofSeconds(60);

    private static final ContainerLocator LOCATOR = ContainerLocator.locateContainerWithLabels(AUTHORIZER_PORT,
            TopazContainer.DEV_SERVICE_LABEL);

    @BuildStep
    void startTopaz(LaunchModeBuildItem launchMode, DockerStatusBuildItem dockerStatus,
            DevServicesComposeProjectBuildItem compose, List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            TopazBuildTimeConfig config, DevServicesConfig devServicesConfig, OutputTargetBuildItem output,
            BuildProducer<DevServicesResultBuildItem> result) {
        TopazDevServicesConfig cfg = config.devservices();
        if (!cfg.enabled()) {
            LOG.debug("Not starting Dev Services for Topaz: disabled in the config");
            return;
        }
        if (ConfigUtils.isPropertyNonEmpty(AUTHORIZER_HOST) || ConfigUtils.isPropertyNonEmpty(DIRECTORY_HOST)) {
            LOG.debugf("Not starting Dev Services for Topaz: %s or %s is set", AUTHORIZER_HOST, DIRECTORY_HOST);
            return;
        }
        if (cfg.policy().directory().isPresent() && cfg.policy().image().isPresent()) {
            throw new ConfigurationException("quarkus.topaz.devservices.policy.directory and"
                    + " quarkus.topaz.devservices.policy.image are mutually exclusive");
        }

        Path dir = output.getOutputDirectory().resolve("topaz-devservices");
        LocationResolver resolver = new LocationResolver(dir);
        Optional<Path> policy = cfg.policy().directory().map(l -> resolver.resolve("policy.directory", l, "policies"));
        Optional<Path> manifest = cfg.manifestLocation()
                .map(l -> resolver.resolve("manifest-location", l, "manifest.yaml"));
        Optional<Path> data = cfg.dataLocation().map(l -> resolver.resolve("data-location", l, "data"));
        Optional<List<ImportRequest>> requests = data.map(TopazSeeder::requests);

        if (!dockerStatus.isContainerRuntimeAvailable()) {
            LOG.warnf("Dev Services for Topaz needs a container runtime: configure %s and %s, or start Docker/Podman",
                    AUTHORIZER_HOST, DIRECTORY_HOST);
            return;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                sharedNetwork);
        String image = cfg.imageName().orElseGet(() -> getDefaultImageNameFor("topaz"));
        LaunchMode mode = launchMode.getLaunchMode();

        DevServicesResultBuildItem discovered = discover(compose, cfg, image, mode, useSharedNetwork, manifest);
        if (discovered != null) {
            result.produce(discovered);
            return;
        }

        Path configFile = TopazConfigFile.write(dir, cfg);
        Duration timeout = devServicesConfig.timeout().orElse(DEFAULT_STARTUP_TIMEOUT);

        Map<String, Function<TopazContainer, String>> provided = new HashMap<>();
        provided.put(AUTHORIZER_HOST, TopazContainer::appHost);
        provided.put(AUTHORIZER_PORT_KEY, c -> String.valueOf(c.appPort(AUTHORIZER_PORT)));
        provided.put(DIRECTORY_HOST, TopazContainer::appHost);
        provided.put(DIRECTORY_PORT_KEY, c -> String.valueOf(c.appPort(DIRECTORY_PORT)));
        provided.put(PLAIN_TEXT, c -> "true");
        cfg.apiKey().ifPresent(k -> provided.put(API_KEY, c -> k));
        if (cfg.consoleEnabled()) {
            provided.put(CONSOLE_URL, TopazContainer::consoleUrl);
        }

        result.produce(DevServicesResultBuildItem.owned()
                .feature(FEATURE)
                .serviceName(cfg.serviceName())
                .serviceConfig(cfg)
                .startable(() -> new TopazContainer(
                        DockerImageName.parse(image).asCompatibleSubstituteFor("ghcr.io/aserto-dev/topaz"),
                        configFile, policy, cfg, timeout, compose.getDefaultNetworkId(), useSharedNetwork)
                        .withSharedServiceLabel(mode, cfg.serviceName()))
                .postStartHook(c -> {
                    Vertx vertx = Vertx.vertx();
                    try (TopazClient client = c.hostClient(vertx)) {
                        TopazSeeder.seed(client, manifest, requests, data);
                    } finally {
                        vertx.close().toCompletionStage().toCompletableFuture().join();
                    }
                })
                .configProvider(provided)
                .build());
    }

    /** A labelled shared container (dev mode) or a Compose service, if one runs; {@code null} otherwise. */
    private static DevServicesResultBuildItem discover(DevServicesComposeProjectBuildItem compose,
            TopazDevServicesConfig cfg, String image, LaunchMode mode, boolean useSharedNetwork,
            Optional<Path> manifest) {
        String host;
        int authorizerPort;
        int directoryPort;
        Optional<Integer> consolePort;
        String containerId;
        boolean reachableFromHere = true;

        Optional<ContainerAddress> shared = LOCATOR.locateContainer(cfg.serviceName(), cfg.shared(), mode);
        if (shared.isPresent()) {
            Optional<Integer> dirPort = LOCATOR.locatePublicPort(cfg.serviceName(), cfg.shared(), mode, DIRECTORY_PORT);
            if (dirPort.isEmpty()) {
                LOG.warnf("Shared Topaz container %s does not publish port %d; starting a new one",
                        shared.get().getId(), DIRECTORY_PORT);
                return null;
            }
            host = shared.get().getHost();
            authorizerPort = shared.get().getPort();
            directoryPort = dirPort.get();
            consolePort = LOCATOR.locatePublicPort(cfg.serviceName(), cfg.shared(), mode, CONSOLE_PORT);
            containerId = shared.get().getId();
        } else {
            Optional<ContainerAddress> composed = ComposeLocator.locateContainer(compose, List.of(image),
                    AUTHORIZER_PORT, mode, useSharedNetwork);
            if (composed.isEmpty()) {
                return null;
            }
            ContainerAddress a = composed.get();
            host = a.getHost();
            authorizerPort = a.getPort();
            if (useSharedNetwork) {
                directoryPort = DIRECTORY_PORT;
                consolePort = Optional.empty();
                reachableFromHere = false;
            } else {
                Optional<Integer> dirPort = a.getRunningContainer().getPortMapping(DIRECTORY_PORT);
                if (dirPort.isEmpty()) {
                    LOG.warnf("Compose Topaz service %s does not publish port %d; starting a new one", a.getId(),
                            DIRECTORY_PORT);
                    return null;
                }
                directoryPort = dirPort.get();
                consolePort = a.getRunningContainer().getPortMapping(CONSOLE_PORT);
            }
            containerId = a.getId();
        }

        if (manifest.isPresent() && reachableFromHere) {
            Vertx vertx = Vertx.vertx();
            TopazClient.Builder b = TopazClient.builder().authorizer(host, authorizerPort)
                    .directory(host, directoryPort).plainText(true).vertx(vertx);
            cfg.apiKey().ifPresent(b::apiKey);
            try (TopazClient client = b.build()) {
                TopazSeeder.checkDrift(client, manifest.get());
            } finally {
                vertx.close().toCompletionStage().toCompletableFuture().join();
            }
        }

        Map<String, String> conf = new HashMap<>();
        conf.put(AUTHORIZER_HOST, host);
        conf.put(AUTHORIZER_PORT_KEY, String.valueOf(authorizerPort));
        conf.put(DIRECTORY_HOST, host);
        conf.put(DIRECTORY_PORT_KEY, String.valueOf(directoryPort));
        conf.put(PLAIN_TEXT, "true");
        cfg.apiKey().ifPresent(k -> conf.put(API_KEY, k));
        if (cfg.consoleEnabled()) {
            consolePort.ifPresent(p -> conf.put(CONSOLE_URL, "http://" + host + ":" + p));
        }
        return DevServicesResultBuildItem.discovered().feature(FEATURE).containerId(containerId).config(conf).build();
    }
}
