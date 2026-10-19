package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.Structs;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import com.google.protobuf.Struct;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

class DefaultResourceContextMapperTest {

    private final DefaultResourceContextMapper mapper = new DefaultResourceContextMapper();

    @Test
    void pathParamsAndQueryParams() {
        RoutingContext ctx = request(MultiMap.caseInsensitiveMultiMap()
                .add("q", "a").add("tag", "x").add("tag", "y"));
        ResourceMethodInfo info = new ResourceMethodInfo("GET", "/todos/{id}", Map.of("id", "todo-1"));

        Struct struct = mapper.map(ctx, Optional.of(info)).await().indefinitely();

        assertThat(struct).isEqualTo(Structs.of(Map.of(
                "id", "todo-1",
                "q", "a",
                "tag", List.of("x", "y"))));
    }

    @Test
    void pathParamWinsOverQueryParam() {
        RoutingContext ctx = request(MultiMap.caseInsensitiveMultiMap().add("id", "evil"));
        ResourceMethodInfo info = new ResourceMethodInfo("GET", "/todos/{id}", Map.of("id", "todo-1"));

        Struct struct = mapper.map(ctx, Optional.of(info)).await().indefinitely();

        assertThat(struct).isEqualTo(Structs.of(Map.of("id", "todo-1")));
    }

    @Test
    void noMethodInfoGivesQueryOnly() {
        RoutingContext ctx = request(MultiMap.caseInsensitiveMultiMap().add("q", "a"));

        Struct struct = mapper.map(ctx, Optional.empty()).await().indefinitely();

        assertThat(struct).isEqualTo(Structs.of(Map.of("q", "a")));
    }

    private static RoutingContext request(MultiMap query) {
        RoutingContext ctx = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(ctx.request()).thenReturn(request);
        when(ctx.queryParams()).thenReturn(query);
        when(request.params()).thenReturn(query);
        return ctx;
    }
}
