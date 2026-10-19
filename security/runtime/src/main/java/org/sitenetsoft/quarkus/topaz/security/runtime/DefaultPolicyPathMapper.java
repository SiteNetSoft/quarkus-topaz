package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import io.quarkus.arc.DefaultBean;
import io.vertx.ext.web.RoutingContext;

@Singleton
@DefaultBean
public class DefaultPolicyPathMapper implements PolicyPathMapper {

    private final TopazEnforcementConfig config;
    private final String rootPath;

    @Inject
    public DefaultPolicyPathMapper(TopazEnforcementConfig config,
            @ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/") String rootPath) {
        this.config = config;
        this.rootPath = normalize(rootPath);
    }

    @Override
    public String map(RoutingContext request, Optional<ResourceMethodInfo> method) {
        String root = config.policyRoot().orElseThrow(
                () -> new IllegalStateException("quarkus.topaz.enforcement.policy-root is not set"));
        if (method.isPresent()) {
            return PolicyPaths.of(root, method.get().httpMethod(), method.get().pathTemplate());
        }
        String path = stripRootPath(request.normalizedPath());
        checkRawSegments(path);
        return PolicyPaths.of(root, request.request().method().name(), path);
    }

    // Before REST matching the path comes straight from the caller. A '.' would add Rego package segments, and a
    // segment starting with '__' or '{' would name a template policy, so the caller could pick the package.
    private static void checkRawSegments(String path) {
        for (String segment : path.split("/")) {
            String s = segment.trim();
            if (s.contains(".") || s.startsWith("__") || s.startsWith("{")) {
                throw new IllegalStateException("Policy path cannot be derived from request path " + path
                        + ": segment " + s + " is not allowed before REST matching; bind the policy with"
                        + " applies-to=jaxrs or @AuthorizationPolicy");
            }
        }
    }

    private String stripRootPath(String path) {
        if (!rootPath.isEmpty() && (path.equals(rootPath) || path.startsWith(rootPath + "/"))) {
            return path.substring(rootPath.length());
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
