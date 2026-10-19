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

/** The Topaz Dev Service container; the Dev Services framework starts and closes it. */
final class TopazContainer extends GenericContainer<TopazContainer> implements Startable {

    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-topaz";

    private final boolean consoleEnabled;
    private final boolean useSharedNetwork;
    private final String hostName;
    private final Optional<String> apiKey;

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
        // Always published: the post-start seeding runs on the host even when the app joins a shared network.
        withExposedPorts(AUTHORIZER_PORT, DIRECTORY_PORT, HEALTH_PORT);
        if (consoleEnabled) {
            addExposedPort(CONSOLE_PORT);
        }
        waitingFor(new TopazReadyWaitStrategy(apiKey).withStartupTimeout(startupTimeout));
    }

    TopazContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
        return ConfigureUtil.configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
    }

    /** Host the application connects to. */
    String appHost() {
        return useSharedNetwork ? hostName : getHost();
    }

    int appPort(int containerPort) {
        return useSharedNetwork ? containerPort : getMappedPort(containerPort);
    }

    boolean consoleEnabled() {
        return consoleEnabled;
    }

    /** Console URL for a browser on the host, so always the published port. */
    String consoleUrl() {
        return "http://" + getHost() + ":" + getMappedPort(CONSOLE_PORT);
    }

    /** A client reaching the container from this JVM (published ports), for seeding. */
    TopazClient hostClient(Vertx vertx) {
        TopazClient.Builder b = TopazClient.builder()
                .authorizer(getHost(), getMappedPort(AUTHORIZER_PORT))
                .directory(getHost(), getMappedPort(DIRECTORY_PORT))
                .plainText(true)
                .vertx(vertx);
        apiKey.ifPresent(b::apiKey);
        return b.build();
    }

    @Override
    public String getConnectionInfo() {
        return appHost() + ":" + appPort(AUTHORIZER_PORT);
    }

    @Override
    public void close() {
        super.close();
    }
}
