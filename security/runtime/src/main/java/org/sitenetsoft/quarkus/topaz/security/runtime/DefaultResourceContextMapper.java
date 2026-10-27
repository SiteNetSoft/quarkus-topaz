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
 * The default {@link ResourceContextMapper}, which builds the resource context from the request parameters.
 * <p>
 * The context holds the JAX-RS path parameters, when the route is known, and the query parameters. The mapper never
 * reads the request body.
 * <p>
 * It is a {@code @DefaultBean}. To replace it, provide your own bean implementing {@link ResourceContextMapper}.
 */
@Singleton
@DefaultBean
public class DefaultResourceContextMapper implements ResourceContextMapper {

    /**
     * Create the DefaultResourceContextMapper.
     */
    public DefaultResourceContextMapper() {
    }

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
     * Report whether this mapper may block.
     * The built-in mapper does not block. A subclass counts as blocking unless it overrides this method.
     *
     * @return {@code false} for the built-in class, {@code true} for a subclass
     */
    @Override
    public boolean blocking() {
        return getClass() != DefaultResourceContextMapper.class;
    }
}
