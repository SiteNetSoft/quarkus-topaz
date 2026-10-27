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
 * The default {@link PolicyPathMapper}, which builds the policy path in the Aserto middleware convention.
 * <p>
 * The convention is {@code <policy-root>.<METHOD>.<path>}. The mapper strips {@code quarkus.http.root-path} and
 * {@code quarkus.rest.path} from the path. It then replaces {@code /} with {@code .} and {@code {param}} with
 * {@code __param}.
 * <p>
 * It is a {@code @DefaultBean}. To replace it, provide your own bean implementing {@link PolicyPathMapper}. With a
 * custom mapper, {@code quarkus.topaz.enforcement.policy-root} is no longer required.
 */
@Singleton
@DefaultBean
public class DefaultPolicyPathMapper implements PolicyPathMapper {

    private final TopazEnforcementConfig config;
    private final String rootPath;
    private final String restPath;

    /**
     * Create the mapper.
     *
     * @param config the enforcement configuration
     * @param rootPath the value of {@code quarkus.http.root-path}
     * @param restPath the value of {@code quarkus.rest.path}
     */
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
     * Report whether this mapper may block.
     * The built-in mapper does not block. A subclass counts as blocking unless it overrides this method.
     *
     * @return {@code false} for the built-in class, {@code true} for a subclass
     */
    @Override
    public boolean blocking() {
        return getClass() != DefaultPolicyPathMapper.class;
    }

    // Reject path segments that would let the caller pick the Rego package.
    // Before REST matching, the path comes straight from the caller.
    // A '.' would add package segments.
    // A segment starting with '__' or '{' would name a template policy.
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

    // The raw path still carries quarkus.http.root-path and then quarkus.rest.path. The policy path has neither.
    private static String strip(String prefix, String path) {
        if (!prefix.isEmpty() && (path.equals(prefix) || path.startsWith(prefix + "/"))) {
            return path.substring(prefix.length());
        }
        return path;
    }

    /** Return the root path with a leading and no trailing {@code /}, or empty for {@code /}. */
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
