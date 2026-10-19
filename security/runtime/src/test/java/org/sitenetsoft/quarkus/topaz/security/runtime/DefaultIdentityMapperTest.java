package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.IdentityType;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

class DefaultIdentityMapperTest {

    private final DefaultIdentityMapper mapper = new DefaultIdentityMapper();

    @Test
    void anonymousIsNone() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        IdentityContext context = map(identity);

        assertThat(context.getType()).isEqualTo(IdentityType.IDENTITY_TYPE_NONE);
    }

    @Test
    void tokenCredentialIsJwt() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(() -> "rick@the-citadel.com")
                .addCredential(new TokenCredential("x.y.z", "bearer"))
                .build();

        IdentityContext context = map(identity);

        assertThat(context.getType()).isEqualTo(IdentityType.IDENTITY_TYPE_JWT);
        assertThat(context.getIdentity()).isEqualTo("x.y.z");
    }

    @Test
    void principalIsSub() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(() -> "rick@the-citadel.com")
                .build();

        IdentityContext context = map(identity);

        assertThat(context.getType()).isEqualTo(IdentityType.IDENTITY_TYPE_SUB);
        assertThat(context.getIdentity()).isEqualTo("rick@the-citadel.com");
    }

    private IdentityContext map(SecurityIdentity identity) {
        return mapper.map(identity, Optional.empty()).await().indefinitely();
    }
}
