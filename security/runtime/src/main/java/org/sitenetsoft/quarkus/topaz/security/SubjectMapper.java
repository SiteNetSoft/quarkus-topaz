package org.sitenetsoft.quarkus.topaz.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Maps the caller's {@link SecurityIdentity} to a directory subject id, e.g. for relation checks.
 * <p>
 * The default fails with {@link io.quarkus.security.UnauthorizedException} for an anonymous identity, returns the
 * {@code sub} claim when the principal is a MicroProfile {@code JsonWebToken}, and the principal name otherwise. A
 * {@code JsonWebToken} without a {@code sub} claim also fails with {@code UnauthorizedException}: it cannot identify a
 * Topaz subject, and the principal name is not used in its place.
 * Provide a CDI bean of this type to replace it.
 * <p>
 * Threading: {@code map} runs on a thread that may block, unless the mapper returns {@code false} from
 * {@link #blocking()}. The {@code quarkus.topaz.enforcement.timeout} bounds the whole check, and a mapper that runs
 * out of time yields a denial. A mapper must not use {@code @RequestScoped} beans.
 */
public interface SubjectMapper {

    /**
     * @param identity the caller's identity
     * @return the subject id; a failure denies the request
     */
    Uni<String> map(SecurityIdentity identity);

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
