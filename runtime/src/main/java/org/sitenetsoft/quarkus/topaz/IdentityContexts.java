package org.sitenetsoft.quarkus.topaz;

import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.IdentityType;

/**
 * Factory methods for {@link IdentityContext}.
 */
public final class IdentityContexts {

    private IdentityContexts() {
    }

    /**
     * Create the identity of an anonymous caller.
     *
     * @return the anonymous identity
     */
    public static IdentityContext none() {
        return IdentityContext.newBuilder().setType(IdentityType.IDENTITY_TYPE_NONE).build();
    }

    /**
     * Create the identity of a caller named by a subject.
     * The directory resolves the subject, for example a user key or an email.
     *
     * @param subject the subject of the caller
     * @return the subject identity
     */
    public static IdentityContext subject(String subject) {
        return of(IdentityType.IDENTITY_TYPE_SUB, subject);
    }

    /**
     * Create the identity of a caller named by a JWT.
     * Topaz does not validate the token and does not check its signature.
     * It parses the token and resolves the {@code sub} claim through the directory.
     * Authenticate the request in your application, for example with quarkus-oidc, before you forward the token.
     * Pass the raw token only so that Topaz can read its subject.
     *
     * @param token the raw JWT
     * @return the JWT identity
     */
    public static IdentityContext jwt(String token) {
        return of(IdentityType.IDENTITY_TYPE_JWT, token);
    }

    /**
     * Create an identity that Topaz passes verbatim to the policy.
     * The directory does not resolve it.
     *
     * @param identity the identity string
     * @return the manual identity
     */
    public static IdentityContext manual(String identity) {
        return of(IdentityType.IDENTITY_TYPE_MANUAL, identity);
    }

    private static IdentityContext of(IdentityType type, String identity) {
        return IdentityContext.newBuilder().setType(type)
                .setIdentity(java.util.Objects.requireNonNull(identity, "identity")).build();
    }
}
