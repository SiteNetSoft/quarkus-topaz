package org.sitenetsoft.quarkus.topaz.security.deployment;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.Structs;
import org.sitenetsoft.quarkus.topaz.security.ResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import com.google.protobuf.Struct;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/** Puts the request body, as the mapper sees it, into the resource context. */
@Singleton
public class BodyContextMapper implements ResourceContextMapper {

    @Override
    public Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method) {
        String body = request.body() == null || request.body().asString() == null ? "" : request.body().asString();
        return Uni.createFrom().item(Structs.of(Map.of("body", body)));
    }
}
