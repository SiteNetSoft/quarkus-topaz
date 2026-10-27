package org.sitenetsoft.quarkus.topaz.deployment;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.AuthorizerClient;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.sitenetsoft.quarkus.topaz.runtime.TopazRecorder;

import org.sitenetsoft.quarkus.topaz.runtime.health.TopazHealthCheck;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

/**
 * Build steps of the Topaz extension.
 */
class TopazProcessor {

    /** Name of the extension feature. */
    static final String FEATURE = "topaz";

    /** Create the processor. */
    TopazProcessor() {
    }

    /**
     * Register the {@code topaz} feature.
     *
     * @return the feature build item
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Enable SSL support in native mode.
     *
     * @return the SSL support build item
     */
    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    /**
     * Index the generated gRPC stubs.
     *
     * @return the index dependency build item
     */
    @BuildStep
    IndexDependencyBuildItem indexStubs() {
        return new IndexDependencyBuildItem("org.sitenetsoft", "quarkus-topaz-grpc");
    }

    /**
     * Register the client beans.
     * The client classes are final and cannot be client-proxied, so the beans are {@code @Singleton}.
     *
     * @param recorder the recorder that creates the clients
     * @param vertx the Vert.x build item
     * @param tls the TLS registry build item
     * @param shutdown the shutdown context
     * @param beans the producer of synthetic beans
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void beans(TopazRecorder recorder, VertxBuildItem vertx, TlsRegistryBuildItem tls,
            ShutdownContextBuildItem shutdown, BuildProducer<SyntheticBeanBuildItem> beans) {
        RuntimeValue<TopazClient> client = recorder.createClient(vertx.getVertx(), tls.registry(), shutdown);
        beans.produce(SyntheticBeanBuildItem.configure(TopazClient.class).scope(Singleton.class)
                .setRuntimeInit().unremovable().runtimeValue(client).done());
        beans.produce(SyntheticBeanBuildItem.configure(AuthorizerClient.class).scope(Singleton.class)
                .setRuntimeInit().unremovable().runtimeValue(recorder.authorizer(client)).done());
        beans.produce(SyntheticBeanBuildItem.configure(DirectoryClient.class).scope(Singleton.class)
                .setRuntimeInit().unremovable().runtimeValue(recorder.directory(client)).done());
    }

    /**
     * Register the readiness health check when SmallRye Health is present.
     *
     * @param capabilities the capabilities of the application
     * @param buildTimeConfig the build-time configuration
     * @param health the producer of health build items
     */
    @BuildStep
    void healthCheck(Capabilities capabilities, TopazBuildTimeConfig buildTimeConfig,
            BuildProducer<HealthBuildItem> health) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            health.produce(new HealthBuildItem(TopazHealthCheck.class.getName(), buildTimeConfig.health().enabled()));
        }
    }
}
