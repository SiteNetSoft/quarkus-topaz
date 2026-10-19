package org.sitenetsoft.quarkus.topaz.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Maps the caller's {@link SecurityIdentity} to a directory subject id, e.g. for relation checks.
 * <p>
 * The default fails with {@link io.quarkus.security.UnauthorizedException} for an anonymous identity, returns the
 * {@code sub} claim when the principal is a MicroProfile {@code JsonWebToken}, and the principal name otherwise.
 * Provide a CDI bean of this type to replace it.
 */
public interface SubjectMapper {

    /**
     * @param identity the caller's identity
     * @return the subject id; a failure denies the request
     */
    Uni<String> map(SecurityIdentity identity);
}
