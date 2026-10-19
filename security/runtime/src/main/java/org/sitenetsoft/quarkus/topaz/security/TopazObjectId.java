package org.sitenetsoft.quarkus.topaz.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks where a {@link TopazCheck} reads its object id.
 * <p>
 * On a method parameter, the argument is the object id; at most one parameter of a method may carry it. On a field or a
 * no-argument getter (including a record component, which propagates to its accessor), the value is read from the first
 * argument whose class declares it, when no parameter is annotated. The value is converted with {@code toString()}; a
 * null or blank value denies the call.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
public @interface TopazObjectId {
}
