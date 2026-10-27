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
 * The default {@link SubjectMapper}, which resolves the subject id used by {@code @TopazCheck}.
 * <p>
 * The subject id is the {@code sub} claim of a JWT when one is present and the principal name otherwise. An anonymous
 * caller fails with {@code UnauthorizedException}.
 * <p>
 * It is a {@code @DefaultBean}. To replace it, provide your own bean implementing {@link SubjectMapper}.
 */
@Singleton
@DefaultBean
public class DefaultSubjectMapper implements SubjectMapper {

    private static final boolean JWT_PRESENT = isJwtPresent();

    /**
     * Create the DefaultSubjectMapper.
     */
    public DefaultSubjectMapper() {
    }

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

    /**
     * Report whether this mapper may block.
     * The built-in mapper does not block. A subclass counts as blocking unless it overrides this method.
     *
     * @return {@code false} for the built-in class, {@code true} for a subclass
     */
    @Override
    public boolean blocking() {
        return getClass() != DefaultSubjectMapper.class;
    }

    /**
     * Tell whether the MicroProfile JWT API is on the classpath.
     *
     * @return {@code true} when the API is present
     */
    private static boolean isJwtPresent() {
        try {
            Class.forName("org.eclipse.microprofile.jwt.JsonWebToken", false, DefaultSubjectMapper.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Isolate the {@link JsonWebToken} reference so it links only when the API is present. */
    private static final class Jwt {

        static boolean isJwt(Principal principal) {
            return principal instanceof JsonWebToken;
        }

        static String subject(Principal principal) {
            return ((JsonWebToken) principal).getSubject();
        }
    }
}
