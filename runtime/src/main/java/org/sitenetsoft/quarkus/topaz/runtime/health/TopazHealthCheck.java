package org.sitenetsoft.quarkus.topaz.runtime.health;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.sitenetsoft.quarkus.topaz.runtime.ResolvedServiceSettings;
import org.sitenetsoft.quarkus.topaz.runtime.TopazConfig;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;

/**
 * Readiness check: the authorizer answers {@code info} and the directory answers {@code getManifest}
 * (a directory without a manifest, NOT_FOUND, is still reachable).
 */
@Readiness
@ApplicationScoped
public class TopazHealthCheck implements AsyncHealthCheck {

    static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(10);

    @Inject
    TopazClient client;

    @Inject
    TopazConfig config;

    @Override
    public Uni<HealthCheckResponse> call() {
        try {
            String authorizer = endpoint(ResolvedServiceSettings.resolve(config, config.authorizer(), 8282));
            String directory = endpoint(ResolvedServiceSettings.resolve(config, config.directory(), 9292));
            Uni<?> info = client.authorizer().info();
            Uni<?> manifest = client.directory().getManifest()
                    .onFailure(TopazHealthCheck::isNotFound).recoverWithItem("");
            return Uni.combine().all().unis(info, manifest).discardItems()
                    .ifNoItem().after(HEALTH_TIMEOUT).fail()
                    .map(ignored -> HealthCheckResponse.named("Topaz").up()
                            .withData("authorizer", authorizer).withData("directory", directory).build())
                    .onFailure().recoverWithItem(TopazHealthCheck::down);
        } catch (RuntimeException e) {
            return Uni.createFrom().item(down(e));
        }
    }

    private static String endpoint(ResolvedServiceSettings settings) {
        return settings.host() + ":" + settings.port();
    }

    private static boolean isNotFound(Throwable t) {
        return t instanceof StatusRuntimeException e && e.getStatus().getCode() == Status.Code.NOT_FOUND;
    }

    private static HealthCheckResponse down(Throwable t) {
        String message = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
        return HealthCheckResponse.named("Topaz").down().withData("error", message).build();
    }
}
