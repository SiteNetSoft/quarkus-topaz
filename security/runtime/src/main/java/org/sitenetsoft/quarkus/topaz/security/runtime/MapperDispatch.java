package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * A dispatcher that runs the mapper calls of an enforcement point under the enforcement timeout.
 * <p>
 * Every enforcement point calls its mappers through {@link #run}. A mapper may block. The timeout bounds the whole
 * assembly, mappers included.
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

    private final TopazEnforcementConfig config;

    /**
     * Create the dispatcher.
     *
     * @param config the enforcement configuration that holds the timeout
     */
    @Inject
    MapperDispatch(TopazEnforcementConfig config) {
        this.config = config;
    }

    /**
     * Run the assembly of an enforcement point under the enforcement timeout.
     * <p>
     * When the caller cannot block, the assembly runs on a worker thread through the current Vert.x context. The
     * duplicated context then stays current on the worker. The item comes back on the caller's Vert.x context when
     * there is one. When the caller may block, the assembly runs on the subscribing thread. The timeout still turns the
     * result into a denial. Nothing interrupts a blocking assembly, so on a blocking method the 403 waits for it to
     * return.
     *
     * @param <T> the item type
     * @param assembly the supplier that calls the mappers and returns the result
     * @param point the enforcement point that calls the mappers
     * @param callerMayBlock whether the subscribing thread may block
     * @return the item of the assembly, or a {@link io.smallrye.mutiny.TimeoutException} failure on a timeout
     */
    <T> Uni<T> run(Supplier<Uni<T>> assembly, Point point, boolean callerMayBlock) {
        if (callerMayBlock) {
            return Uni.createFrom().voidItem()
                    .chain(assembly::get)
                    .ifNoItem().after(config.timeout()).fail();
        }
        Context context = Vertx.currentContext();
        // The assembly runs on a worker after the subscription. The timeout is then armed while the mappers run,
        // so it bounds a mapper that blocks or throws instead of emitting. A deferred supplier would run before the
        // timer starts. On the event loop, a blocking mapper would also stop the timer.
        Uni<T> result = Uni.createFrom().voidItem()
                .emitOn(executor(context))
                .chain(assembly::get)
                .ifNoItem().after(config.timeout()).fail();
        // A cache hit completes on the worker and a timeout on the timer thread. Resume on the caller's context.
        return context == null ? result : result.emitOn(task -> context.runOnContext(v -> task.run()));
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
