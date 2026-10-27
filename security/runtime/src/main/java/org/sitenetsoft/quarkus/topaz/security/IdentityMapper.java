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
 * Threading: {@code map} runs on a thread that may block, unless the mapper returns {@code false} from
 * {@link #blocking()}. During an HTTP request, the request's Vert.x duplicated context stays current. The {@code
 * quarkus.topaz.enforcement.timeout} bounds the whole evaluation, and a mapper that runs out of time yields a denial.
 * A mapper must not use {@code @RequestScoped} beans. The {@code RoutingContext} is not thread-safe, so do not pass it
 * to other threads.
 */
public interface IdentityMapper {

    /**
     * @param identity the caller's identity
     * @param request the current HTTP request, empty outside an HTTP request
     * @return the identity context; a failure denies the request
     */
    Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request);

    /**
     * Tell whether {@code map} may block.
     * <p>
     * A mapper that returns {@code false} must not block and must not use {@code @RequestScoped} beans. When no
     * component of an enforcement point blocks, the mappers run on the calling thread. That thread can be the event
     * loop. The default returns {@code true}, so {@code map} runs on a thread that may block.
     *
     * @return {@code true} when {@code map} may block, {@code false} when it never blocks
     */
    default boolean blocking() {
        return true;
    }
}
