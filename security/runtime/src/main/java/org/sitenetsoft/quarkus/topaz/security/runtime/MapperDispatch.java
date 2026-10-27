package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.DecisionCache;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.PolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfoResolver;
import org.sitenetsoft.quarkus.topaz.security.SubjectMapper;

import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * A dispatcher that runs the mapper calls of an enforcement point under the enforcement timeout.
 * <p>
 * Every enforcement point calls its mappers through {@link #run}. A mapper may block, unless it returns {@code false}
 * from {@code blocking()}. The timeout bounds the whole assembly, mappers included.
 */
@Singleton
final class MapperDispatch {

    /**
     * An enforcement point that calls the mappers.
     */
    enum Point {

        /**
         * The HTTP route policy.
         */
        ROUTE,

        /**
         * The {@code @TopazAuthorize} interceptor.
         */
        AUTHORIZE,

        /**
         * The {@code @TopazCheck} interceptor.
         */
        CHECK,

        /**
         * The decision permission of the {@code @PermissionsAllowed} bridge.
         */
        BRIDGE_DECISION,

        /**
         * The relation permission of the {@code @PermissionsAllowed} bridge.
         */
        BRIDGE_RELATION
    }

    private static final String REST_RESOLVER = "org.sitenetsoft.quarkus.topaz.security.runtime.rest"
            + ".RestResourceMethodInfoResolver";

    private final TopazEnforcementConfig config;
    private final Set<Point> fastPath;

    /**
     * Create the dispatcher and decide which enforcement points run their mappers on the calling thread.
     *
     * @param config the enforcement configuration that holds the timeout
     * @param identityMapper the identity mapper in use
     * @param policyPathMapper the policy path mapper in use
     * @param resourceContextMapper the resource context mapper in use
     * @param subjectMapper the subject mapper in use
     * @param cache the decision cache in use
     * @param resolver the resource method resolver in use
     */
    @Inject
    MapperDispatch(TopazEnforcementConfig config, IdentityMapper identityMapper, PolicyPathMapper policyPathMapper,
            ResourceContextMapper resourceContextMapper, SubjectMapper subjectMapper, DecisionCache cache,
            ResourceMethodInfoResolver resolver) {
        this.config = config;
        boolean identity = !identityMapper.blocking();
        boolean path = !policyPathMapper.blocking();
        boolean resource = !resourceContextMapper.blocking();
        boolean subject = !subjectMapper.blocking();
        boolean noCache = cache instanceof NoopDecisionCache;
        // Referenced by name so this class never loads the REST resolver, which needs quarkus-rest on the classpath.
        boolean builtInResolver = resolver instanceof NoopResourceMethodInfoResolver
                || resolver.getClass().getName().equals(REST_RESOLVER);
        Set<Point> points = EnumSet.noneOf(Point.class);
        if (builtInResolver && path && identity && resource && noCache) {
            points.add(Point.ROUTE);
            points.add(Point.AUTHORIZE);
        }
        if (subject) {
            points.add(Point.CHECK);
            points.add(Point.BRIDGE_RELATION);
        }
        if (identity && noCache) {
            points.add(Point.BRIDGE_DECISION);
        }
        this.fastPath = Collections.unmodifiableSet(points);
    }

    /**
     * Run the assembly of an enforcement point under the enforcement timeout.
     * <p>
     * The subscribing thread picks one of three branches. When no component of the point blocks, the assembly runs on
     * the subscribing thread. Otherwise, when the caller and the subscribing thread may both block, the assembly also
     * runs on the subscribing thread. Otherwise the assembly runs on a worker thread through the current Vert.x
     * context, so the duplicated context stays current on the worker.
     * <p>
     * The timeout turns a late result into a denial in every branch. Nothing interrupts an assembly that blocks on the
     * subscribing thread, so on a blocking method the 403 waits for it to return. The item comes back on the
     * subscriber's Vert.x context when there is one.
     *
     * @param <T> the item type
     * @param assembly the supplier that calls the mappers and returns the result
     * @param point the enforcement point that calls the mappers
     * @param callerMayBlock whether the caller allows the assembly to run on the subscribing thread
     * @return the item of the assembly, or a {@link io.smallrye.mutiny.TimeoutException} failure on a timeout
     */
    <T> Uni<T> run(Supplier<Uni<T>> assembly, Point point, boolean callerMayBlock) {
        return Uni.createFrom().deferred(() -> {
            Context context = Vertx.currentContext();
            Uni<Void> start = Uni.createFrom().voidItem();
            if (!fastPath.contains(point) && !(callerMayBlock && BlockingOperationControl.isBlockingAllowed())) {
                start = start.emitOn(executor(context));
            }
            // The assembly runs after the subscription. The timeout is then armed while the mappers run, so it
            // bounds a mapper that blocks or throws instead of emitting.
            Uni<T> result = start.chain(assembly::get)
                    .ifNoItem().after(config.timeout()).fail();
            // A cache hit can complete on the worker and a timeout on the timer thread. Resume on the caller's context.
            return context == null ? result : result.emitOn(task -> context.runOnContext(v -> task.run()));
        });
    }

    // Blocking work runs through the current Vert.x context, so the duplicated context stays current on the worker.
    // The request scope, the reactive clients and the identity association depend on it. Without a Vert.x context,
    // the default worker pool runs the work.
    private static Executor executor(Context context) {
        if (context == null) {
            return Infrastructure.getDefaultWorkerPool();
        }
        return task -> context.executeBlocking(() -> {
            task.run();
            return null;
        }, false);
    }
}
