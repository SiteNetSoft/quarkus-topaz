package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import io.quarkus.arc.DefaultBean;
import io.vertx.ext.web.RoutingContext;

/**
 * Builds the policy path in the Aserto middleware convention, {@code <policy-root>.<METHOD>.<path>}, with {@code /}
 * replaced by {@code .} and {@code {param}} by {@code __param}, after stripping {@code quarkus.http.root-path} and
 * {@code quarkus.rest.path} from the path. It is a {@code @DefaultBean}: to replace it, provide your own bean
 * implementing {@link PolicyPathMapper}; with a custom mapper {@code quarkus.topaz.enforcement.policy-root} is no
 * longer required.
 */
@Singleton
@DefaultBean
public class DefaultPolicyPathMapper implements PolicyPathMapper {

    private final TopazEnforcementConfig config;
    private final String rootPath;
    private final String restPath;

    @Inject
    public DefaultPolicyPathMapper(TopazEnforcementConfig config,
            @ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/") String rootPath,
            @ConfigProperty(name = "quarkus.rest.path", defaultValue = "/") String restPath) {
        this.config = config;
        this.rootPath = normalize(rootPath);
        this.restPath = normalize(restPath);
    }

    @Override
    public String map(RoutingContext request, Optional<ResourceMethodInfo> method) {
        String root = config.policyRoot().orElseThrow(
                () -> new IllegalStateException("quarkus.topaz.enforcement.policy-root is not set"));
        if (method.isPresent()) {
            return PolicyPaths.of(root, method.get().httpMethod(), method.get().pathTemplate());
        }
        String path = strip(restPath, strip(rootPath, request.normalizedPath()));
        checkRawSegments(path);
        return PolicyPaths.of(root, request.request().method().name(), path);
    }

    /**
     * Report that this mapper never blocks.
     *
     * @return {@code false}
     */
    @Override
    public boolean blocking() {
        return false;
    }

    // Before REST matching the path comes straight from the caller. A '.' would add Rego package segments, and a
    // segment starting with '__' or '{' would name a template policy, so the caller could pick the package.
    private static void checkRawSegments(String path) {
        for (String segment : path.split("/")) {
            String s = segment.trim();
            if (s.contains(".") || s.startsWith("__") || s.contains("{")
                    || s.contains("}")) {
                throw new IllegalStateException("Policy path cannot be derived from request path " + path
                        + ": segment " + s + " is not allowed before REST matching; bind the policy with"
                        + " applies-to=jaxrs or @AuthorizationPolicy");
            }
        }
    }

    // The raw path still carries quarkus.http.root-path and then quarkus.rest.path; the policy path has neither.
    private static String strip(String prefix, String path) {
        if (!prefix.isEmpty() && (path.equals(prefix) || path.startsWith(prefix + "/"))) {
            return path.substring(prefix.length());
        }
        return path;
    }

    /** Returns the root path with a leading and no trailing {@code /}, or empty for {@code /}. */
    private static String normalize(String rootPath) {
        String p = rootPath.trim();
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.isEmpty()) {
            return "";
        }
        return p.startsWith("/") ? p : "/" + p;
    }
}
