package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Dev Services for Topaz: a Topaz container started in dev and test mode when no authorizer or directory host is
 * configured.
 */
@ConfigGroup
public interface TopazDevServicesConfig {

    /**
     * Whether Dev Services for Topaz is enabled. It is skipped anyway when
     * {@code quarkus.topaz.authorizer.host} or {@code quarkus.topaz.directory.host} is set, or when no container
     * runtime is available.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The Topaz container image. Defaults to the image this extension was tested with.
     */
    Optional<String> imageName();

    /**
     * Whether the container is shared between applications in dev mode. A shared container is found through the
     * {@code quarkus-dev-service-topaz} label and is not re-seeded.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * Value of the {@code quarkus-dev-service-topaz} label, used to find a shared container.
     */
    @WithDefault("topaz")
    String serviceName();

    /**
     * Where the policy comes from.
     */
    PolicyConfig policy();

    /**
     * Location of a directory manifest YAML loaded after start, as {@code classpath:}, {@code filesystem:} or a
     * plain path.
     */
    Optional<String> manifestLocation();

    /**
     * Location of a directory holding {@code objects.json} and/or {@code relations.json} imported after start, as
     * {@code classpath:}, {@code filesystem:} or a plain path.
     */
    Optional<String> dataLocation();

    /**
     * When set, the container requires this API key (except for {@code Info} and gRPC reflection) and the
     * application is configured to send it.
     */
    Optional<String> apiKey();

    /**
     * Environment variables passed to the container.
     */
    @ConfigDocMapKey("env-name")
    Map<String, String> containerEnv();

    /**
     * Whether the Topaz console is exposed; its URL is set as {@code quarkus.topaz.console-url}.
     */
    @WithDefault("true")
    boolean consoleEnabled();

    @ConfigGroup
    interface PolicyConfig {

        /**
         * Directory of Rego files mounted as a local bundle, as {@code classpath:}, {@code filesystem:} or a plain
         * path. Mutually exclusive with {@code image}.
         */
        Optional<String> directory();

        /**
         * OCI policy image pulled from {@code ghcr.io}, e.g. {@code ghcr.io/aserto-policies/policy-todo:latest}.
         * Mutually exclusive with {@code directory}.
         */
        Optional<String> image();
    }
}
