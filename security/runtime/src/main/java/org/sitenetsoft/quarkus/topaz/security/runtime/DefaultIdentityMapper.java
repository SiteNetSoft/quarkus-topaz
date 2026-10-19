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

@Singleton
@DefaultBean
public class DefaultIdentityMapper implements IdentityMapper {

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
}
