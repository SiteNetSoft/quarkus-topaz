package org.sitenetsoft.quarkus.topaz.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Requires a Topaz directory relation check to pass before the annotated CDI bean method (or every business method of
 * the annotated class) runs: the caller must have {@link #relation()} (or a permission of that name) on the object
 * {@link #objectType()}:{@code <object id>}.
 * <p>
 * The subject is the current {@code SecurityIdentity}, mapped to a subject id of type {@link #subjectType()} by the
 * {@link SubjectMapper}. The object id is {@link #objectId()} when set; otherwise the method argument annotated
 * {@link TopazObjectId}; otherwise the {@link TopazObjectId} field or getter of the first argument whose class declares
 * one.
 * <p>
 * An anonymous caller is rejected with {@code UnauthorizedException} (401). A failing check, a directory error, a
 * timeout ({@code quarkus.topaz.enforcement.timeout}), a mapper failure or an object id that cannot be resolved (none,
 * null or blank) raises {@code ForbiddenException} (403); an error never permits.
 * <p>
 * A method returning {@code Uni} or {@code Multi} is checked on subscription. Any other method, including one returning
 * {@code CompletionStage}, is treated as blocking: it waits for the check before running, so it must run on a worker
 * thread; on the event loop it fails with {@code IllegalStateException}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TopazCheck {

    /**
     * The object type in the directory, for example {@code todo}.
     */
    @Nonbinding
    String objectType();

    /**
     * The relation or permission the subject must have on the object, for example {@code owner} or
     * {@code can_delete}.
     */
    @Nonbinding
    String relation();

    /**
     * The subject type of the caller in the directory.
     */
    @Nonbinding
    String subjectType() default "user";

    /**
     * A fixed object id. When empty, the object id is read from the method arguments through {@link TopazObjectId}.
     */
    @Nonbinding
    String objectId() default "";
}
