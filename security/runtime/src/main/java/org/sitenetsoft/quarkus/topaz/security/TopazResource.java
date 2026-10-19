package org.sitenetsoft.quarkus.topaz.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a method argument to the resource context of a {@link TopazAuthorize} check, under the key {@link #value()}.
 * <p>
 * The argument is converted with Jackson: strings, numbers, booleans, maps and lists pass through, other objects become
 * maps of their properties. The value replaces a key of the same name produced by the {@link ResourceContextMapper}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TopazResource {

    /**
     * The key in the resource context, read by policies as {@code input.resource.<key>}.
     */
    String value();
}
