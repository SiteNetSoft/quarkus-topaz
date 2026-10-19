package org.sitenetsoft.quarkus.topaz.security.runtime.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RestResourceMethodInfoResolverTest {

    @ParameterizedTest
    @CsvSource({
            "/todos, {id}, /todos/{id}",
            "/todos/, /{id}, /todos/{id}",
            "todos, '', /todos",
            "/todos, '', /todos",
            "'', '', /",
            "/, /, /",
            "/todos, '{id:[0-9]{3}}', '/todos/{id:[0-9]{3}}'" })
    void joinTemplates(String classPath, String methodPath, String expected) {
        assertThat(RestResourceMethodInfoResolver.joinTemplates(classPath, methodPath)).isEqualTo(expected);
    }
}
