package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.time.Duration;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Shared plumbing of the Topaz interceptors: the current identity and request, the reactive or blocking dispatch, and
 * the mapping of a denial to a security exception.
 */
@Singleton
public class InterceptorSupport {

    private static final Logger LOG = Logger.getLogger(InterceptorSupport.class);

    static final String EVENT_LOOP_MESSAGE = "@TopazAuthorize/@TopazCheck on a blocking method cannot run on the event"
            + " loop; return Uni or run on a worker thread";

    private final CurrentIdentityAssociation identity;
    private final CurrentVertxRequest request;

    @Inject
    InterceptorSupport(CurrentIdentityAssociation identity, CurrentVertxRequest request) {
        this.identity = identity;
        this.request = request;
    }

    /**
     * @return the current identity, resolved lazily
     */
    public Uni<SecurityIdentity> identity() {
        return identity.getDeferredIdentity();
    }

    /**
     * @return the current HTTP request; empty outside an HTTP request
     */
    public Optional<RoutingContext> request() {
        if (!Arc.container().requestContext().isActive()) {
            return Optional.empty();
        }
        return Optional.ofNullable(request.getCurrent());
    }

    /**
     * Turns a check into a security exception: {@link UnauthorizedException} and {@link ForbiddenException} pass
     * through, any other failure is logged and becomes a {@link ForbiddenException}. The check fails with a
     * {@link ForbiddenException} when it does not complete within {@code timeout}.
     *
     * @param check the check
     * @param what what is checked, for the log
     * @param timeout the deadline of the whole check
     * @return the guarded check
     */
    public static Uni<Void> failClosed(Uni<Void> check, String what, Duration timeout) {
        return check
                .ifNoItem().after(timeout).fail()
                .onFailure(e -> !(e instanceof UnauthorizedException || e instanceof ForbiddenException))
                .transform(e -> {
                    LOG.warnf("Topaz denied %s: %s", what, e.toString());
                    return new ForbiddenException();
                });
    }

    /**
     * Runs the intercepted method once {@code check} succeeds.
     * <p>
     * A method returning {@link Uni} or {@link Multi} is checked on subscription. Any other method blocks on the check,
     * which requires a thread that may block.
     *
     * @param ctx the invocation
     * @param check completes when the call is permitted; fails with {@link UnauthorizedException} or
     *        {@link ForbiddenException} otherwise
     * @param timeout the check's own deadline; a blocking wait gives up one second after it
     * @return the method's result
     * @throws Exception the method's exception, or the check's
     */
    public static Object dispatch(InvocationContext ctx, Uni<Void> check, Duration timeout) throws Exception {
        Class<?> returnType = ctx.getMethod().getReturnType();
        if (Uni.class.isAssignableFrom(returnType)) {
            return check.chain(() -> {
                try {
                    return (Uni<?>) ctx.proceed();
                } catch (Exception e) {
                    return Uni.createFrom().failure(e);
                }
            });
        }
        if (Multi.class.isAssignableFrom(returnType)) {
            return Multi.createFrom().deferred(() -> check.onItem().transformToMulti(v -> {
                try {
                    return (Multi<?>) ctx.proceed();
                } catch (Exception e) {
                    return Multi.createFrom().failure(e);
                }
            }));
        }
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new IllegalStateException(EVENT_LOOP_MESSAGE);
        }
        try {
            check.await().atMost(timeout.plusSeconds(1));
        } catch (TimeoutException e) {
            LOG.warnf("Topaz denied %s: %s", ctx.getMethod(), e.toString());
            throw new ForbiddenException();
        }
        return ctx.proceed();
    }
}
