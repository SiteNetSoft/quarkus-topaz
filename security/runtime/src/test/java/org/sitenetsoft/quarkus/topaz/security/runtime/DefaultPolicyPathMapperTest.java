package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

class DefaultPolicyPathMapperTest {

    @Test
    void usesMethodTemplate() {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config("todoApp"), "/");
        ResourceMethodInfo info = new ResourceMethodInfo("DELETE", "/todos/{id}", Map.of("id", "todo-1"));

        assertThat(mapper.map(request(HttpMethod.DELETE, "/todos/todo-1"), Optional.of(info)))
                .isEqualTo("todoApp.DELETE.todos.__id");
    }

    @Test
    void stripsRootPathFromRawPath() {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config("todoApp"), "/api");

        assertThat(mapper.map(request(HttpMethod.GET, "/api/todos"), Optional.empty()))
                .isEqualTo("todoApp.GET.todos");
    }

    @Test
    void stripsRootPathWithoutLeadingSlash() {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config("todoApp"), "api");

        assertThat(mapper.map(request(HttpMethod.GET, "/api/todos"), Optional.empty()))
                .isEqualTo("todoApp.GET.todos");
    }

    @Test
    void missingPolicyRootFails() {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config(null), "/");

        assertThatThrownBy(() -> mapper.map(request(HttpMethod.GET, "/todos"), Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quarkus.topaz.enforcement.policy-root");
    }

    private static TopazEnforcementConfig config(String policyRoot) {
        TopazEnforcementConfig config = mock(TopazEnforcementConfig.class);
        when(config.policyRoot()).thenReturn(Optional.ofNullable(policyRoot));
        return config;
    }

    private static RoutingContext request(HttpMethod method, String path) {
        RoutingContext ctx = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(ctx.request()).thenReturn(request);
        when(request.method()).thenReturn(method);
        when(ctx.normalizedPath()).thenReturn(path);
        return ctx;
    }
}
