package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.IdentityContexts;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;

import aserto.authorizer.v2.api.IdentityContext;
import io.quarkus.arc.DefaultBean;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * The default {@link IdentityMapper}, which maps the {@link SecurityIdentity} to a Topaz identity context.
 * <p>
 * The type is {@code JWT} with the raw token when the identity carries a {@link TokenCredential}. Otherwise the type is
 * {@code SUB} with the principal name when the identity is authenticated, and {@code NONE} when it is not.
 * <p>
 * It is a {@code @DefaultBean}. To replace it, provide your own bean implementing {@link IdentityMapper}.
 */
@Singleton
@DefaultBean
public class DefaultIdentityMapper implements IdentityMapper {

    /**
     * Create the DefaultIdentityMapper.
     */
    public DefaultIdentityMapper() {
    }

    @Override
    public Uni<IdentityContext> map(SecurityIdentity identity, Optional<RoutingContext> request) {
        try {
            if (identity.isAnonymous()) {
                return Uni.createFrom().item(IdentityContexts.none());
            }
            TokenCredential token = identity.getCredential(TokenCredential.class);
            if (token != null) {
                return Uni.createFrom().item(IdentityContexts.jwt(token.getToken()));
            }
            return Uni.createFrom().item(IdentityContexts.subject(identity.getPrincipal().getName()));
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
        return getClass() != DefaultIdentityMapper.class;
    }
}
