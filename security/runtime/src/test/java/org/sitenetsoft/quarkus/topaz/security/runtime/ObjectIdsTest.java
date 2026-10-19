package org.sitenetsoft.quarkus.topaz.security.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.security.TopazCheck;
import org.sitenetsoft.quarkus.topaz.security.TopazObjectId;

class ObjectIdsTest {

    static class FieldOnly {
        @TopazObjectId
        private final String id;

        FieldOnly(String id) {
            this.id = id;
        }
    }

    static class GetterOnly {
        @TopazObjectId
        public String key() {
            return "getter-id";
        }
    }

    static class Base {
        @TopazObjectId
        String id = "base-id";
    }

    static class Derived extends Base {
    }

    record Ref(@TopazObjectId String id, String note) {
    }

    static class Fixtures {
        @TopazCheck(objectType = "todo", relation = "owner", objectId = "fixed")
        void constant(@TopazObjectId String id) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void parameter(String other, @TopazObjectId String id) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void twoParameters(@TopazObjectId String a, @TopazObjectId String b) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void fieldOnly(String other, FieldOnly value) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void getterOnly(GetterOnly value) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void superclass(Derived value) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void record(Ref value) {
        }

        @TopazCheck(objectType = "todo", relation = "owner")
        void none(String id, Object other) {
        }
    }

    private static String resolve(String name, Object... args) {
        Method method = Arrays.stream(Fixtures.class.getDeclaredMethods()).filter(m -> m.getName().equals(name))
                .findFirst().orElseThrow();
        return ObjectIds.resolve(method.getAnnotation(TopazCheck.class), method, args);
    }

    @Test
    void constantWins() {
        assertThat(resolve("constant", "argument")).isEqualTo("fixed");
    }

    @Test
    void annotatedParameter() {
        assertThat(resolve("parameter", "other", "todo-1")).isEqualTo("todo-1");
    }

    @Test
    void twoAnnotatedParametersFail() {
        assertThatThrownBy(() -> resolve("twoParameters", "a", "b")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fixtures#twoParameters");
    }

    @Test
    void nullParameterFails() {
        assertThatThrownBy(() -> resolve("parameter", "other", null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fixtures#parameter");
    }

    @Test
    void blankParameterFails() {
        assertThatThrownBy(() -> resolve("parameter", "other", "  ")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fixtures#parameter");
    }

    @Test
    void privateFieldWithoutGetter() {
        assertThat(resolve("fieldOnly", "other", new FieldOnly("field-id"))).isEqualTo("field-id");
    }

    @Test
    void getterWithoutField() {
        assertThat(resolve("getterOnly", new GetterOnly())).isEqualTo("getter-id");
    }

    @Test
    void declaredOnSuperclass() {
        assertThat(resolve("superclass", new Derived())).isEqualTo("base-id");
    }

    @Test
    void recordComponent() {
        assertThat(resolve("record", new Ref("todo-1", "x"))).isEqualTo("todo-1");
    }

    @Test
    void nullArgumentWithMemberFails() {
        assertThatThrownBy(() -> resolve("record", (Object) null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fixtures#record");
    }

    @Test
    void noSourceFails() {
        assertThatThrownBy(() -> resolve("none", "todo-1", new Object())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fixtures#none");
    }
}
