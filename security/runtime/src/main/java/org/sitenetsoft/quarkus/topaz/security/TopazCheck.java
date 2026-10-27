package org.sitenetsoft.quarkus.topaz.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Requires a Topaz directory relation check to pass before the annotated CDI bean method runs.
 * <p>
 * On a class, the annotation covers every business method of the class. The caller must have {@link #relation()}, or a
 * permission of that name, on the object {@link #objectType()}:{@code <object id>}.
 * <p>
 * The subject is the current {@code SecurityIdentity}, mapped to a subject id of type {@link #subjectType()} by the
 * {@link SubjectMapper}. The object id is {@link #objectId()} when set. Otherwise it is the method argument annotated
 * {@link TopazObjectId}. Otherwise it is the {@link TopazObjectId} field or getter of the first argument whose declared
 * parameter type, or one of its superclasses, declares one. The lookup uses the declared parameter type and not the
 * runtime class of the argument. A {@link TopazObjectId} declared only on a subclass is not found.
 * <p>
 * An anonymous caller is always rejected with {@code UnauthorizedException}, which is HTTP 401. The value of
 * {@code quarkus.topaz.enforcement.anonymous} does not matter, because a relation check needs a subject.
 * <p>
 * Each of these raises {@code ForbiddenException}, which is HTTP 403:
 * <ul>
 * <li>a failing check
 * <li>a directory error
 * <li>a timeout, set by {@code quarkus.topaz.enforcement.timeout}
 * <li>a mapper failure
 * <li>an object id that cannot be resolved because it is missing, null or blank
 * </ul>
 * An error never permits.
 * <p>
 * A method returning {@code Uni} or {@code Multi} is checked on subscription. Any other method, including one returning
 * {@code CompletionStage}, is treated as blocking. It waits for the check before running, so it must run on a worker
 * thread. On the event loop it fails with {@code IllegalStateException}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TopazCheck {

    /**
     * Get the object type in the directory, for example {@code todo}.
     *
     * @return the object type
     */
    @Nonbinding
    String objectType();

    /**
     * Get the relation or permission the subject must have on the object.
     * <p>
     * Examples are {@code owner} and {@code can_delete}.
     *
     * @return the relation or permission
     */
    @Nonbinding
    String relation();

    /**
     * Get the subject type of the caller in the directory.
     *
     * @return the subject type
     */
    @Nonbinding
    String subjectType() default "user";

    /**
     * Get a fixed object id.
     * <p>
     * When empty, the object id is read from the method arguments through {@link TopazObjectId}.
     *
     * @return the fixed object id, or empty to read it from the arguments
     */
    @Nonbinding
    String objectId() default "";
}
