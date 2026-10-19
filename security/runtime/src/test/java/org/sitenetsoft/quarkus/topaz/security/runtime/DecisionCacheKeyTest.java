package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import aserto.authorizer.v2.api.IdentityContext;
import aserto.authorizer.v2.api.IdentityType;

class DecisionCacheKeyTest {

    private static final String TOKEN_A = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyaWNrIn0.c2lnbmF0dXJlLWE";
    private static final String TOKEN_B = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtb3J0eSJ9.c2lnbmF0dXJlLWI";

    @Test
    void keyIsAnOpaqueDigest() {
        String key = DecisionEvaluator.cacheKey(jwt(TOKEN_A), "todoApp.GET.todos", "allowed", "{\"id\":\"todo-1\"}");

        assertThat(key).matches("^[0-9a-f]{64}$")
                .doesNotContain(TOKEN_A)
                .doesNotContain("eyJ")
                .doesNotContain("todoApp")
                .doesNotContain("todo-1");
    }

    @Test
    void keyDiffersPerToken() {
        assertThat(DecisionEvaluator.cacheKey(jwt(TOKEN_A), "todoApp.GET.todos", "allowed", "{}"))
                .isNotEqualTo(DecisionEvaluator.cacheKey(jwt(TOKEN_B), "todoApp.GET.todos", "allowed", "{}"));
    }

    @Test
    void keyIsStable() {
        assertThat(DecisionEvaluator.cacheKey(jwt(TOKEN_A), "todoApp.GET.todos", "allowed", "{}"))
                .isEqualTo(DecisionEvaluator.cacheKey(jwt(TOKEN_A), "todoApp.GET.todos", "allowed", "{}"));
    }

    private static IdentityContext jwt(String token) {
        return IdentityContext.newBuilder().setType(IdentityType.IDENTITY_TYPE_JWT).setIdentity(token).build();
    }
}
