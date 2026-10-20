package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.runtime.Internal;

class InternalPackageTest {

    @Test
    void runtimePackageIsMarkedInternal() throws ClassNotFoundException {
        assertThat(Class.forName("org.sitenetsoft.quarkus.topaz.security.runtime.package-info").getPackage()
                .isAnnotationPresent(Internal.class)).isTrue();
    }
}
