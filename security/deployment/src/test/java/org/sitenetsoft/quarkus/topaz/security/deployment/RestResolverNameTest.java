package org.sitenetsoft.quarkus.topaz.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.security.runtime.rest.RestResourceMethodInfoResolver;

public class RestResolverNameTest {

    @Test
    void processorNameMatchesClass() throws Exception {
        assertThat(read(TopazSecurityProcessor.class)).isEqualTo(RestResourceMethodInfoResolver.class.getName());
    }

    @Test
    void dispatchNameMatchesClass() throws Exception {
        Class<?> dispatch = Class.forName("org.sitenetsoft.quarkus.topaz.security.runtime.MapperDispatch");
        assertThat(read(dispatch)).isEqualTo(RestResourceMethodInfoResolver.class.getName());
    }

    private static String read(Class<?> owner) throws Exception {
        Field field = owner.getDeclaredField("REST_RESOLVER");
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
