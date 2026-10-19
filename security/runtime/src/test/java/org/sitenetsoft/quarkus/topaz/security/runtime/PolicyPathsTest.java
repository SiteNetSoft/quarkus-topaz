package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolicyPathsTest {

    @ParameterizedTest
    @CsvSource({
            "todoApp, GET, /todos, todoApp.GET.todos",
            "todoApp, DELETE, /todos/{id}, todoApp.DELETE.todos.__id",
            "todoApp, PUT, /todos/{id:[0-9]+}/items/{itemId}, todoApp.PUT.todos.__id.items.__itemId",
            "todoApp, GET, /, todoApp.GET",
            "todoApp, GET, todos/, todoApp.GET.todos" })
    void convention(String root, String method, String path, String expected) {
        assertThat(PolicyPaths.of(root, method, path)).isEqualTo(expected);
    }
}
