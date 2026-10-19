package org.sitenetsoft.quarkus.topaz;

import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.IdentityType;

/**
 * Factory methods for {@link IdentityContext}.
 */
public final class IdentityContexts {

    private IdentityContexts() {
    }

    /** Anonymous caller. */
    public static IdentityContext none() {
        return IdentityContext.newBuilder().setType(IdentityType.IDENTITY_TYPE_NONE).build();
    }

    /** Caller identified by a subject (user key, email, ...) resolved by the directory. */
    public static IdentityContext subject(String subject) {
        return of(IdentityType.IDENTITY_TYPE_SUB, subject);
    }

    /**
     * Caller identified by a JWT. Topaz does not validate the token: it does not check the signature, it parses the
     * token and resolves its {@code sub} claim through the directory. Authenticate the request in your application
     * (e.g. quarkus-oidc) before forwarding the token; pass the raw token only so Topaz can read its subject.
     */
    public static IdentityContext jwt(String token) {
        return of(IdentityType.IDENTITY_TYPE_JWT, token);
    }

    /** Caller identity passed verbatim to the policy, without directory resolution. */
    public static IdentityContext manual(String identity) {
        return of(IdentityType.IDENTITY_TYPE_MANUAL, identity);
    }

    private static IdentityContext of(IdentityType type, String identity) {
        return IdentityContext.newBuilder().setType(type)
                .setIdentity(java.util.Objects.requireNonNull(identity, "identity")).build();
    }
}
