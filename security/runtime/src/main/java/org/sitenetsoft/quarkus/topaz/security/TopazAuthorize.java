package org.sitenetsoft.quarkus.topaz.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Requires a permitting Topaz decision before the annotated CDI bean method (or every business method of the annotated
 * class) runs.
 * <p>
 * The caller is the current {@code SecurityIdentity}, mapped by the {@link IdentityMapper}. An anonymous caller is
 * rejected with {@code UnauthorizedException} (401) when {@code quarkus.topaz.enforcement.anonymous=deny}; otherwise it is
 * evaluated with an empty identity. A denying decision, an authorizer error, a timeout or a mapper failure raises
 * {@code ForbiddenException} (403); an error never permits.
 * <p>
 * The resource context is the {@link ResourceContextMapper} output for the current HTTP request (empty outside one),
 * overlaid with the method arguments annotated {@link TopazResource}.
 * <p>
 * A method returning {@code Uni} or {@code Multi} is checked on subscription. Any other method, including one returning
 * {@code CompletionStage}, is treated as blocking: it waits for the decision before running, so it must run on a worker
 * thread; on the event loop it fails with {@code IllegalStateException}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TopazAuthorize {

    /**
     * The policy path, for example {@code todoApp.DELETE.todos.__id}. When empty, the path is derived from the current
     * HTTP request with the {@link PolicyPathMapper}; this requires {@code quarkus.topaz.enforcement.policy-root} and a
     * JAX-RS resource class (annotated {@code @Path}), which the build verifies.
     */
    @Nonbinding
    String path() default "";

    /**
     * The decision that must be true. Empty means {@code quarkus.topaz.enforcement.decision}.
     */
    @Nonbinding
    String decision() default "";

    /**
     * Further decisions to request alongside {@link #decision()}. When empty, the configured
     * {@code quarkus.topaz.enforcement.additional-decisions} are requested instead.
     */
    @Nonbinding
    String[] decisions() default {};
}
