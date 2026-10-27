package org.sitenetsoft.quarkus.topaz.runtime.devui;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.sitenetsoft.quarkus.topaz.TopazClient;
import org.sitenetsoft.quarkus.topaz.runtime.ResolvedServiceSettings;
import org.sitenetsoft.quarkus.topaz.runtime.TopazConfig;
import org.sitenetsoft.quarkus.topaz.runtime.health.TopazHealthCheck;

import io.smallrye.mutiny.Uni;

/**
 * Dev UI JSON-RPC backend of the Topaz card. Only registered in local development.
 */
@ApplicationScoped
public class TopazJsonRpcService {

    public record Connection(String authorizer, String directory, String consoleUrl) {
    }

    public record Policy(String id, String packagePath) {
    }

    @Inject
    TopazClient client;

    @Inject
    TopazConfig config;

    public Connection connection() {
        ResolvedServiceSettings a = ResolvedServiceSettings.resolve(config, config.authorizer(), 8282);
        ResolvedServiceSettings d = ResolvedServiceSettings.resolve(config, config.directory(), 9292);
        return new Connection(a.host() + ":" + a.port(), d.host() + ":" + d.port(),
                config.consoleUrl().orElse(null));
    }

    public Uni<List<Policy>> listPolicies() {
        return client.authorizer().listPolicies()
                .ifNoItem().after(TopazHealthCheck.HEALTH_TIMEOUT).failWith(TopazJsonRpcService::stalled)
                .map(modules -> modules.stream().map(m -> new Policy(m.getId(), m.getPackagePath())).toList());
    }

    public Uni<String> getManifest() {
        return client.directory().getManifest()
                .ifNoItem().after(TopazHealthCheck.HEALTH_TIMEOUT).failWith(TopazJsonRpcService::stalled);
    }

    private static Throwable stalled() {
        return new IllegalStateException(
                "Topaz did not answer within " + TopazHealthCheck.HEALTH_TIMEOUT.toSeconds() + "s");
    }
}
