package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.AUTHORIZER_PORT;
import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.CONSOLE_PORT;
import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.DIRECTORY_PORT;
import static org.sitenetsoft.quarkus.topaz.deployment.devservices.TopazConfigFile.HEALTH_PORT;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.runtime.LaunchMode;
import io.vertx.core.Vertx;

/**
 * The Topaz Dev Service container.
 * The Dev Services framework starts and closes it.
 */
final class TopazContainer extends GenericContainer<TopazContainer> implements Startable {

    /** Label that marks the container as the Topaz Dev Service. */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-topaz";

    private final boolean consoleEnabled;
    private final boolean useSharedNetwork;
    private final String hostName;
    private final Optional<String> apiKey;

    /**
     * Create the container.
     *
     * @param image the Topaz image
     * @param configFile the config file to copy into the container
     * @param policyDir the policy directory to copy into the container, if any
     * @param cfg the Dev Services configuration
     * @param startupTimeout how long to wait for Topaz to become ready
     * @param defaultNetworkId the default network id of the Compose project
     * @param useSharedNetwork whether the application joins a shared network
     */
    TopazContainer(DockerImageName image, Path configFile, Optional<Path> policyDir, TopazDevServicesConfig cfg,
            Duration startupTimeout, String defaultNetworkId, boolean useSharedNetwork) {
        super(image);
        this.consoleEnabled = cfg.consoleEnabled();
        this.useSharedNetwork = useSharedNetwork;
        this.apiKey = cfg.apiKey();
        this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "topaz");
        withEnv("TOPAZ_DB_DIR", "/db");
        withEnv("TOPAZ_CERTS_DIR", "/certs");
        withEnv("TOPAZ_DECISIONS_DIR", "/decisions");
        withEnv(cfg.containerEnv());
        withCopyFileToContainer(MountableFile.forHostPath(configFile), TopazConfigFile.CONTAINER_PATH);
        policyDir.ifPresent(d -> withCopyFileToContainer(MountableFile.forHostPath(d), TopazConfigFile.POLICIES_PATH));
        // Always publish these ports, because the seeding runs on the host even when the application joins a shared network.
        withExposedPorts(AUTHORIZER_PORT, DIRECTORY_PORT, HEALTH_PORT);
        if (consoleEnabled) {
            addExposedPort(CONSOLE_PORT);
        }
        waitingFor(new TopazReadyWaitStrategy(apiKey).withStartupTimeout(startupTimeout));
    }

    /**
     * Label the container so that other applications can find and share it.
     *
     * @param launchMode the launch mode
     * @param serviceName the value of the service label
     * @return this container
     */
    TopazContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
        return ConfigureUtil.configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
    }

    /**
     * Get the host the application connects to.
     *
     * @return the host
     */
    String appHost() {
        return useSharedNetwork ? hostName : getHost();
    }

    /**
     * Get the port the application connects to.
     *
     * @param containerPort the port inside the container
     * @return the container port on a shared network, otherwise the mapped port
     */
    int appPort(int containerPort) {
        return useSharedNetwork ? containerPort : getMappedPort(containerPort);
    }

    /**
     * Check whether the console is exposed.
     *
     * @return {@code true} if the console is exposed
     */
    boolean consoleEnabled() {
        return consoleEnabled;
    }

    /**
     * Get the console URL for a browser on the host.
     * The URL always uses the published port.
     *
     * @return the console URL
     */
    String consoleUrl() {
        return "http://" + getHost() + ":" + getMappedPort(CONSOLE_PORT);
    }

    /**
     * Create a client that reaches the container from this JVM through the published ports.
     * The seeding uses it.
     *
     * @param vertx the Vert.x instance of the client
     * @return the client
     */
    TopazClient hostClient(Vertx vertx) {
        TopazClient.Builder b = TopazClient.builder()
                .authorizer(getHost(), getMappedPort(AUTHORIZER_PORT))
                .directory(getHost(), getMappedPort(DIRECTORY_PORT))
                .plainText(true)
                .vertx(vertx);
        apiKey.ifPresent(b::apiKey);
        return b.build();
    }

    /** {@inheritDoc} */
    @Override
    public String getConnectionInfo() {
        return appHost() + ":" + appPort(AUTHORIZER_PORT);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        super.close();
    }
}
