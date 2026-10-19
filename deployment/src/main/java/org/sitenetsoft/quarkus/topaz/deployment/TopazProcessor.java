package org.sitenetsoft.quarkus.topaz.deployment;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.AuthorizerClient;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.sitenetsoft.quarkus.topaz.runtime.TopazRecorder;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.tls.deployment.spi.TlsRegistryBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

class TopazProcessor {

    static final String FEATURE = "topaz";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem ssl() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexStubs() {
        return new IndexDependencyBuildItem("org.sitenetsoft", "quarkus-topaz-grpc");
    }

    // The client classes are final, so they cannot be client-proxied: @Singleton instead of @ApplicationScoped.
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
}
