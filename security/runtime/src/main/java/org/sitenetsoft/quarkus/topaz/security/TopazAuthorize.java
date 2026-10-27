package org.sitenetsoft.quarkus.topaz.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Requires a permitting Topaz decision before the annotated CDI bean method runs.
 * <p>
 * On a class, the annotation covers every business method of the class.
 * <p>
 * The caller is the current {@code SecurityIdentity}, mapped by the {@link IdentityMapper}. When
 * {@code quarkus.topaz.enforcement.anonymous=deny}, an anonymous caller is rejected with
 * {@code UnauthorizedException}, which is HTTP 401. Otherwise the anonymous caller is evaluated with an empty identity.
 * A denying decision, an authorizer error, a timeout or a mapper failure raises {@code ForbiddenException}, which is
 * HTTP 403. An error never permits.
 * <p>
 * The resource context is the {@link ResourceContextMapper} output for the current HTTP request (empty outside one),
 * overlaid with the method arguments annotated {@link TopazResource}.
 * <p>
 * A method returning {@code Uni} or {@code Multi} is checked on subscription. Any other method, including one returning
 * {@code CompletionStage}, is treated as blocking. It waits for the decision before running, so it must run on a worker
 * thread. On the event loop it fails with {@code IllegalStateException}.
 */
@InterceptorBinding
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TopazAuthorize {

    /**
     * Get the policy path, for example {@code todoApp.DELETE.todos.__id}.
     * <p>
     * When empty, the {@link PolicyPathMapper} derives the path from the current HTTP request. A derived path always
     * means the policy of the HTTP request being served, so only JAX-RS resource methods may omit it.
     * <p>
     * The build verifies that an annotated method carries an HTTP method annotation. That is {@code @GET},
     * {@code @POST} and so on, or an annotation meta-annotated with {@code @HttpMethod}. The build also verifies that
     * an annotated class is a JAX-RS resource, annotated with {@code @Path} directly or on an implemented interface.
     * It also requires {@code quarkus.topaz.enforcement.policy-root}. Set the path explicitly on any other method.
     *
     * @return the policy path, or empty to derive it from the request
     */
    @Nonbinding
    String path() default "";

    /**
     * Get the decision that must be true.
     * <p>
     * Empty means {@code quarkus.topaz.enforcement.decision}.
     *
     * @return the decision, or empty for the configured decision
     */
    @Nonbinding
    String decision() default "";

    /**
     * Get the further decisions to request alongside {@link #decision()}.
     * <p>
     * When empty, the configured {@code quarkus.topaz.enforcement.additional-decisions} are requested instead.
     *
     * @return the further decisions, or empty for the configured ones
     */
    @Nonbinding
    String[] decisions() default {};
}
