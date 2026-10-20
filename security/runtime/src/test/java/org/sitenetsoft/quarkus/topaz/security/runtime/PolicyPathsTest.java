package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolicyPathsTest {

    @ParameterizedTest
    @CsvSource({
            "todoApp, GET, /todos, todoApp.GET.todos",
            "todoApp, DELETE, /todos/{id}, todoApp.DELETE.todos.__id",
            "todoApp, PUT, /todos/{id:[0-9]+}/items/{itemId}, todoApp.PUT.todos.__id.items.__itemId",
            "todoApp, GET, /, todoApp.GET",
            "todoApp, GET, todos/, todoApp.GET.todos",
            "todoApp, GET, /todos//{id}, todoApp.GET.todos.__id",
            "todoApp, GET, '/todos/{id:[0-9]{3}}', todoApp.GET.todos.__id",
            "todoApp, GET, todos, todoApp.GET.todos",
            "todoApp, get, /todos, todoApp.GET.todos" })
    void convention(String root, String method, String path, String expected) {
        assertThat(PolicyPaths.of(root, method, path)).isEqualTo(expected);
    }

    @Test
    void regexWithSlash() {
        assertThat(PolicyPaths.of("r", "GET", "/files/{path:.+/.+}/meta")).isEqualTo("r.GET.files.__path.meta");
    }

    @Test
    void nestedBracesInRegex() {
        assertThat(PolicyPaths.of("r", "GET", "/a/{id:[0-9]{3}}")).isEqualTo("r.GET.a.__id");
    }
}
