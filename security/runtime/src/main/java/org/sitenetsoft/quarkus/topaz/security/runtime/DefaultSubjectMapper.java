package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.security.Principal;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.sitenetsoft.quarkus.topaz.security.SubjectMapper;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

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
            if (JWT_PRESENT) {
                String subject = Jwt.subject(principal);
                if (subject != null) {
                    return Uni.createFrom().item(subject);
                }
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

        static String subject(Principal principal) {
            return principal instanceof JsonWebToken jwt ? jwt.getSubject() : null;
        }
    }
}
