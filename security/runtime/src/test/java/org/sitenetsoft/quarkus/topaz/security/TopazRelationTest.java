package org.sitenetsoft.quarkus.topaz.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class TopazRelationTest {

    @Test
    void rejectsWhitespace() {
        for (String bad : List.of("todo #owner", "todo#own er", "todo#owner@ user", " todo#owner")) {
            assertThatThrownBy(() -> new TopazRelation(bad, "todo-1")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
