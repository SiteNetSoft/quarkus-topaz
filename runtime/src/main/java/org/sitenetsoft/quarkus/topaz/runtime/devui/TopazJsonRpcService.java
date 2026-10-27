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
 * Dev UI JSON-RPC backend of the Topaz card.
 * It is registered only in local development.
 */
@ApplicationScoped
public class TopazJsonRpcService {

    /** Create the service. The container injects its fields. */
    public TopazJsonRpcService() {
    }

    /**
     * Connection details of the Topaz services.
     *
     * @param authorizer the authorizer endpoint as {@code host:port}
     * @param directory the directory endpoint as {@code host:port}
     * @param consoleUrl the URL of the Topaz console, or {@code null} if unknown
     */
    public record Connection(String authorizer, String directory, String consoleUrl) {
    }

    /**
     * A policy module.
     *
     * @param id the id of the module
     * @param packagePath the package path of the module
     */
    public record Policy(String id, String packagePath) {
    }

    /** The Topaz client that calls the services. */
    @Inject
    TopazClient client;

    /** The configuration that names the services. */
    @Inject
    TopazConfig config;

    /**
     * Get the connection details of the Topaz services.
     *
     * @return the connection details
     */
    public Connection connection() {
        ResolvedServiceSettings a = ResolvedServiceSettings.resolve(config, config.authorizer(), 8282);
        ResolvedServiceSettings d = ResolvedServiceSettings.resolve(config, config.directory(), 9292);
        return new Connection(a.host() + ":" + a.port(), d.host() + ":" + d.port(),
                config.consoleUrl().orElse(null));
    }

    /**
     * List the policy modules of the authorizer.
     * The call fails if Topaz does not answer within {@link TopazHealthCheck#HEALTH_TIMEOUT}.
     *
     * @return the policy modules
     */
    public Uni<List<Policy>> listPolicies() {
        return client.authorizer().listPolicies()
                .ifNoItem().after(TopazHealthCheck.HEALTH_TIMEOUT).failWith(TopazJsonRpcService::stalled)
                .map(modules -> modules.stream().map(m -> new Policy(m.getId(), m.getPackagePath())).toList());
    }

    /**
     * Get the manifest YAML of the directory.
     * The call fails if Topaz does not answer within {@link TopazHealthCheck#HEALTH_TIMEOUT}.
     *
     * @return the manifest YAML
     */
    public Uni<String> getManifest() {
        return client.directory().getManifest()
                .ifNoItem().after(TopazHealthCheck.HEALTH_TIMEOUT).failWith(TopazJsonRpcService::stalled);
    }

    private static Throwable stalled() {
        return new IllegalStateException(
                "Topaz did not answer within " + TopazHealthCheck.HEALTH_TIMEOUT.toSeconds() + "s");
    }
}
