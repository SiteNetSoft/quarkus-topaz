package org.sitenetsoft.quarkus.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.DecisionCache;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;

public class TopazAuthorizeWorkerVerticleTest {

    static final String ADDRESS = "topaz-authorize-worker";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(AuthorizeBoundService.class, WorkerDeployer.class, AuthorizeVerticle.class,
                            AlwaysTrueCache.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.topaz.enforcement.timeout", "2s");

    @Inject
    Vertx vertx;

    @Test
    void cacheHitPermittedFromWorkerVerticle() throws Exception {
        Object reply = vertx.eventBus().request(ADDRESS, "todo-1")
                .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).body();
        assertThat(reply).isEqualTo("deleted todo-1");
    }

    @ApplicationScoped
    public static class WorkerDeployer {

        void deploy(@Observes StartupEvent event, Vertx vertx, AuthorizeVerticle verticle) throws Exception {
            vertx.deployVerticle(verticle, new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @ApplicationScoped
    public static class AuthorizeVerticle extends AbstractVerticle {

        @Inject
        AuthorizeBoundService service;

        @Inject
        CurrentIdentityAssociation identity;

        @Override
        public void start() {
            vertx.eventBus().<String> consumer(ADDRESS, message -> {
                ManagedContext requestContext = Arc.container().requestContext();
                requestContext.activate();
                try {
                    identity.setIdentity(QuarkusSecurityIdentity.builder()
                            .setPrincipal(new QuarkusPrincipal("rick@the-citadel.com")).build());
                    message.reply(service.delete(message.body()));
                } catch (RuntimeException e) {
                    message.fail(500, e.toString());
                } finally {
                    requestContext.terminate();
                }
            });
        }
    }

    @Singleton
    public static class AlwaysTrueCache implements DecisionCache {

        @Override
        public Optional<Boolean> get(String key) {
            return Optional.of(true);
        }

        @Override
        public void put(String key, boolean allowed) {
        }
    }
}
