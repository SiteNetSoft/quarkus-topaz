package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    void literalRawPathIsMapped() {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config("todoApp"), "/");

        assertThat(mapper.map(request(HttpMethod.GET, "/todos/todo-1"), Optional.empty()))
                .isEqualTo("todoApp.GET.todos.todo-1");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/todos.public", "/todos/__id", "/todos/{id}" })
    void rawPathCannotPickThePackage(String path) {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config("todoApp"), "/");

        assertThatThrownBy(() -> mapper.map(request(HttpMethod.GET, path), Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Policy path cannot be derived from request path " + path)
                .hasMessageContaining("applies-to=jaxrs");
    }

    @Test
    void templateModeKeepsTemplateSegments() {
        DefaultPolicyPathMapper mapper = new DefaultPolicyPathMapper(config("todoApp"), "/");
        ResourceMethodInfo info = new ResourceMethodInfo("GET", "/todos/{id}", Map.of("id", "__x.y"));

        assertThat(mapper.map(request(HttpMethod.GET, "/todos/__x.y"), Optional.of(info)))
                .isEqualTo("todoApp.GET.todos.__id");
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
