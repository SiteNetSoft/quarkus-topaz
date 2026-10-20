package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

import aserto.authorizer.v2.api.IdentityContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Maps the caller's {@link SecurityIdentity} to the identity context sent to the Topaz authorizer.
 * <p>
 * The default maps an anonymous identity to {@code IDENTITY_TYPE_NONE}, an identity carrying a
 * {@link io.quarkus.security.credential.TokenCredential} to {@code IDENTITY_TYPE_JWT} with the raw token, and any other
 * identity to {@code IDENTITY_TYPE_SUB} with the principal name. Provide a CDI bean of this type to replace it.
 * <p>
 * Threading: for the {@code topaz} HTTP security policy, {@code map} runs on a worker thread through the request's
 * Vert.x context, so a blocking body is tolerated, request-scoped beans and the Vert.x context stay available, and
 * the whole evaluation is bounded by {@code quarkus.topaz.enforcement.timeout} (a mapper that runs out of time
 * yields a denial). The {@code RoutingContext} is not thread-safe: do not pass it to other threads. When called
 * from {@code @TopazAuthorize} or the permission bridge, {@code map} runs on the calling thread instead, so it
 * should not block there.
 */
public interface IdentityMapper {

    /**
     * @param identity the caller's identity
     * @param request the current HTTP request, empty outside an HTTP request
     * @return the identity context; a failure denies the request
     */
    Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request);
}
