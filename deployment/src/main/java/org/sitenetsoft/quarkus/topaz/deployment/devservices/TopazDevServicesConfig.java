package org.sitenetsoft.quarkus.topaz.deployment.devservices;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Dev Services for Topaz.
 * It starts a Topaz container in dev and test mode when no authorizer or directory host is configured.
 */
@ConfigGroup
public interface TopazDevServicesConfig {

    /**
     * Whether Dev Services for Topaz is enabled.
     * It is skipped anyway when {@code quarkus.topaz.authorizer.host} or {@code quarkus.topaz.directory.host} is set.
     * It is also skipped when no container runtime is available.
     *
     * @return whether Dev Services is enabled
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The Topaz container image.
     * It defaults to the image this extension was tested with.
     *
     * @return the image name, if set
     */
    Optional<String> imageName();

    /**
     * Whether the container is shared between applications in dev mode.
     * A shared container is found through the {@code quarkus-dev-service-topaz} label.
     * Dev Services does not seed it again.
     *
     * @return whether the container is shared
     */
    @WithDefault("true")
    boolean shared();

    /**
     * Value of the {@code quarkus-dev-service-topaz} label, used to find a shared container.
     *
     * @return the service name
     */
    @WithDefault("topaz")
    String serviceName();

    /**
     * Where the policy comes from.
     *
     * @return the policy settings
     */
    PolicyConfig policy();

    /**
     * Location of a directory manifest YAML that Dev Services loads after the start.
     * Use {@code classpath:}, {@code filesystem:} or a plain path.
     *
     * @return the manifest location, if set
     */
    Optional<String> manifestLocation();

    /**
     * Location of a directory that holds {@code objects.json}, {@code relations.json} or both.
     * Dev Services imports them after the start.
     * Use {@code classpath:}, {@code filesystem:} or a plain path.
     *
     * @return the data location, if set
     */
    Optional<String> dataLocation();

    /**
     * The API key of the container.
     * When set, the container requires this API key for every call except {@code Info} and gRPC reflection.
     * The application is configured to send it.
     *
     * @return the API key, if set
     */
    Optional<String> apiKey();

    /**
     * Environment variables passed to the container.
     *
     * @return the environment variables
     */
    @ConfigDocMapKey("env-name")
    Map<String, String> containerEnv();

    /**
     * Whether the Topaz console is exposed.
     * Dev Services sets its URL as {@code quarkus.topaz.console-url}.
     *
     * @return whether the console is exposed
     */
    @WithDefault("true")
    boolean consoleEnabled();

    /**
     * Group of the policy settings.
     */
    @ConfigGroup
    interface PolicyConfig {

        /**
         * Directory of Rego files mounted as a local bundle.
         * Use {@code classpath:}, {@code filesystem:} or a plain path.
         * It excludes {@code image}.
         *
         * @return the policy directory, if set
         */
        Optional<String> directory();

        /**
         * OCI policy image pulled from {@code ghcr.io}, for example {@code ghcr.io/aserto-policies/policy-todo:latest}.
         * It excludes {@code directory}.
         *
         * @return the policy image, if set
         */
        Optional<String> image();
    }
}
