package org.sitenetsoft.quarkus.topaz.security;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void rejectsNonAsciiSpaces() {
        for (String bad : List.of("todo#can\u00a0read", "todo#can\u2003read")) {
            assertThatThrownBy(() -> new TopazRelation(bad, "todo-1")).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("relation");
        }
    }

    @Test
    void rejectsTabAndEmptyParts() {
        for (String bad : List.of("todo#can\tread", "#owner", "todo#", "todo#owner@")) {
            assertThatThrownBy(() -> new TopazRelation(bad, "todo-1")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void acceptsTopazNames() {
        TopazRelation full = new TopazRelation("todo#can_read@user", "todo-1");
        assertThat(full.objectType()).isEqualTo("todo");
        assertThat(full.relation()).isEqualTo("can_read");
        assertThat(full.subjectType()).isEqualTo("user");
        TopazRelation versioned = new TopazRelation("doc-v2#viewer", "doc-1");
        assertThat(versioned.objectType()).isEqualTo("doc-v2");
        assertThat(versioned.relation()).isEqualTo("viewer");
        assertThat(versioned.subjectType()).isEqualTo("user");
    }
}
