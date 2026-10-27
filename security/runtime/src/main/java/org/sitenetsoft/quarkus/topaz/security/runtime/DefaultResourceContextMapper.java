package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.Structs;
import org.sitenetsoft.quarkus.topaz.security.ResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;

import com.google.protobuf.Struct;

import io.quarkus.arc.DefaultBean;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * Builds the resource context from the JAX-RS path parameters, when the route is known, and the query parameters; it
 * never reads the request body. It is a {@code @DefaultBean}: to replace it, provide your own bean implementing {@link
 * ResourceContextMapper}.
 */
@Singleton
@DefaultBean
public class DefaultResourceContextMapper implements ResourceContextMapper {

    @Override
    public Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method) {
        try {
            Map<String, Object> resource = new LinkedHashMap<>();
            method.ifPresent(m -> resource.putAll(m.pathParams()));
            MultiMap query = request.queryParams();
            for (String name : query.names()) {
                if (resource.containsKey(name)) {
                    continue;
                }
                List<String> values = query.getAll(name);
                resource.put(name, values.size() == 1 ? values.get(0) : List.copyOf(values));
            }
            return Uni.createFrom().item(Structs.of(resource));
        } catch (RuntimeException e) {
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Report that this mapper never blocks.
     *
     * @return {@code false}
     */
    @Override
    public boolean blocking() {
        return false;
    }
}
