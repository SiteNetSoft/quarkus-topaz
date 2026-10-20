package org.sitenetsoft.quarkus.topaz;

import java.util.Map;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Builds protobuf {@link Struct}/{@link Value} instances from plain Java values: {@code String}, {@code Number}
 * (converted to {@code double}), {@code Boolean}, {@code null}, {@code Map} (keys converted with
 * {@code String.valueOf}), {@code Iterable}, and already-built {@code Value}/{@code Struct}/{@code ListValue}.
 */
public final class Structs {

    private Structs() {
    }

    /** Converts a map to a {@link Struct}; {@code null} yields an empty struct. */
    public static Struct of(Map<String, ?> map) {
        if (map == null) {
            return Struct.getDefaultInstance();
        }
        return toStruct(map);
    }

    /**
     * Converts a Java value (null, String, Boolean, Number, Map, Iterable, Struct, ListValue or Value) to a protobuf
     * {@link Value}. Other types throw {@link IllegalArgumentException}.
     */
    public static Value value(Object o) {
        if (o == null) {
            return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        }
        if (o instanceof Value v) {
            return v;
        }
        if (o instanceof String s) {
            return Value.newBuilder().setStringValue(s).build();
        }
        if (o instanceof Boolean b) {
            return Value.newBuilder().setBoolValue(b).build();
        }
        if (o instanceof Number n) {
            return Value.newBuilder().setNumberValue(n.doubleValue()).build();
        }
        if (o instanceof Struct s) {
            return Value.newBuilder().setStructValue(s).build();
        }
        if (o instanceof ListValue l) {
            return Value.newBuilder().setListValue(l).build();
        }
        if (o instanceof Map<?, ?> m) {
            return Value.newBuilder().setStructValue(toStruct(m)).build();
        }
        if (o instanceof Iterable<?> it) {
            ListValue.Builder list = ListValue.newBuilder();
            for (Object item : it) {
                list.addValues(value(item));
            }
            return Value.newBuilder().setListValue(list).build();
        }
        throw new IllegalArgumentException("Cannot convert " + o.getClass().getName() + " to a protobuf Value");
    }

    private static Struct toStruct(Map<?, ?> map) {
        Struct.Builder b = Struct.newBuilder();
        map.forEach((k, v) -> b.putFields(String.valueOf(k), value(v)));
        return b.build();
    }
}
