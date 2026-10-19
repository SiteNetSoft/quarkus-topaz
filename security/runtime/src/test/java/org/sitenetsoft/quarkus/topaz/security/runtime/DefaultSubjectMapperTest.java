package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

class DefaultSubjectMapperTest {

    private final DefaultSubjectMapper mapper = new DefaultSubjectMapper();

    @Test
    void anonymousFails() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        assertThatThrownBy(() -> mapper.map(identity).await().indefinitely())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void jwtPrincipalGivesSubject() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(jwt("rick@the-citadel.com")).build();

        assertThat(mapper.map(identity).await().indefinitely()).isEqualTo("rick@the-citadel.com");
    }

    @Test
    void plainPrincipalGivesName() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(() -> "morty@the-citadel.com")
                .build();

        assertThat(mapper.map(identity).await().indefinitely()).isEqualTo("morty@the-citadel.com");
    }

    @Test
    void jwtWithoutSubjectFails() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(jwt(null)).build();

        assertThatThrownBy(() -> mapper.map(identity).await().indefinitely())
                .isInstanceOf(UnauthorizedException.class);
    }

    private static JsonWebToken jwt(String subject) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getName()).thenReturn("principal-name");
        return jwt;
    }
}
