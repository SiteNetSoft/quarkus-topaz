package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.security.Principal;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.sitenetsoft.quarkus.topaz.security.SubjectMapper;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Resolves the subject id used by {@code @TopazCheck}: the principal name, or the {@code sub} claim of a JWT when one
 * is present; an anonymous caller fails with {@code UnauthorizedException}. It is a {@code @DefaultBean}: to replace
 * it, provide your own bean implementing {@link SubjectMapper}.
 */
@Singleton
@DefaultBean
public class DefaultSubjectMapper implements SubjectMapper {

    private static final boolean JWT_PRESENT = isJwtPresent();

    @Override
    public Uni<String> map(SecurityIdentity identity) {
        try {
            if (identity.isAnonymous()) {
                return Uni.createFrom().failure(new UnauthorizedException());
            }
            Principal principal = identity.getPrincipal();
            if (JWT_PRESENT && Jwt.isJwt(principal)) {
                String subject = Jwt.subject(principal);
                if (subject == null || subject.isBlank()) {
                    return Uni.createFrom().failure(new UnauthorizedException("JWT has no subject claim"));
                }
                return Uni.createFrom().item(subject);
            }
            return Uni.createFrom().item(principal.getName());
        } catch (RuntimeException e) {
            return Uni.createFrom().failure(e);
        }
    }

    private static boolean isJwtPresent() {
        try {
            Class.forName("org.eclipse.microprofile.jwt.JsonWebToken", false, DefaultSubjectMapper.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Isolates the {@link JsonWebToken} reference so it is only linked when the API is present. */
    private static final class Jwt {

        static boolean isJwt(Principal principal) {
            return principal instanceof JsonWebToken;
        }

        static String subject(Principal principal) {
            return ((JsonWebToken) principal).getSubject();
        }
    }
}
