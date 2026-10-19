package org.sitenetsoft.quarkus.topaz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

class StructsTest {

    @Test
    void nestedMapAndListRoundTrip() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("n", 42);
        inner.put("ok", true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "rick");
        map.put("nested", inner);
        map.put("tags", List.of("a", 1.5, false));

        Struct s = Structs.of(map);

        assertThat(s.getFieldsOrThrow("name").getStringValue()).isEqualTo("rick");
        Struct nested = s.getFieldsOrThrow("nested").getStructValue();
        assertThat(nested.getFieldsOrThrow("n").getNumberValue()).isEqualTo(42.0);
        assertThat(nested.getFieldsOrThrow("ok").getBoolValue()).isTrue();
        ListValue tags = s.getFieldsOrThrow("tags").getListValue();
        assertThat(tags.getValuesList()).containsExactly(
                Value.newBuilder().setStringValue("a").build(),
                Value.newBuilder().setNumberValue(1.5).build(),
                Value.newBuilder().setBoolValue(false).build());
    }

    @Test
    void nullBecomesNullValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("x", null);

        assertThat(Structs.value(null).getNullValue()).isEqualTo(NullValue.NULL_VALUE);
        assertThat(Structs.value(null).getKindCase()).isEqualTo(Value.KindCase.NULL_VALUE);
        assertThat(Structs.of(map).getFieldsOrThrow("x").getKindCase()).isEqualTo(Value.KindCase.NULL_VALUE);
        assertThat(Structs.value(Arrays.asList("a", null)).getListValue().getValues(1).getKindCase())
                .isEqualTo(Value.KindCase.NULL_VALUE);
    }
}
