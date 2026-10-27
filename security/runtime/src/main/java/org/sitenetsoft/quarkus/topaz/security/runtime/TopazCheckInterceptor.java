package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.lang.reflect.Method;
import java.util.Objects;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.security.SubjectMapper;
import org.sitenetsoft.quarkus.topaz.security.TopazCheck;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * The interceptor that enforces {@link TopazCheck}.
 */
@Interceptor
@TopazCheck(objectType = "", relation = "")
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)
public class TopazCheckInterceptor {

    private static final Logger LOG = Logger.getLogger(TopazCheckInterceptor.class);

    private final InterceptorSupport support;
    private final DirectoryClient directory;
    private final SubjectMapper subjectMapper;
    private final TopazEnforcementConfig config;
    private final MapperDispatch dispatch;

    /**
     * Create the interceptor.
     *
     * @param support the shared interceptor plumbing
     * @param directory the directory client
     * @param subjectMapper the subject mapper
     * @param config the enforcement configuration
     * @param dispatch the mapper dispatcher
     */
    @Inject
    TopazCheckInterceptor(InterceptorSupport support, DirectoryClient directory, SubjectMapper subjectMapper,
            TopazEnforcementConfig config, MapperDispatch dispatch) {
        this.support = support;
        this.directory = directory;
        this.subjectMapper = subjectMapper;
        this.config = config;
        this.dispatch = dispatch;
    }

    /**
     * Check the relation before the intercepted method runs.
     *
     * @param ctx the invocation
     * @return the result of the intercepted method
     * @throws Exception the exception of the intercepted method, or of the check
     */
    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        TopazCheck binding = ctx.getInterceptorBinding(TopazCheck.class);
        Uni<SecurityIdentity> identity = support.identity();
        Object[] args = ctx.getParameters();
        Method method = ctx.getMethod();
        String what = method.getDeclaringClass().getName() + "#" + method.getName();
        // Deny on any failure. failClosed covers the identity lookup and the object id resolution.
        Uni<Void> check = InterceptorSupport.failClosed(identity.chain(id -> {
            if (id.isAnonymous()) {
                return Uni.createFrom().failure(new UnauthorizedException());
            }
            // The inline assembly runs on this thread, so this thread decides whether it may block.
            return dispatch.run(() -> check(binding, id, method, args, what), MapperDispatch.Point.CHECK,
                    InterceptorSupport.blockingDispatch(method));
        }), what, config.timeout());
        return InterceptorSupport.dispatch(ctx, check, config.timeout());
    }

    /**
     * Check the relation for one call.
     *
     * @param binding the annotation
     * @param id the caller's identity
     * @param method the intercepted method
     * @param args the call arguments
     * @param what the method name for the log
     * @return a result that fails with {@link ForbiddenException} when the relation is missing
     */
    private Uni<Void> check(TopazCheck binding, SecurityIdentity id, Method method, Object[] args, String what) {
        String objectId = ObjectIds.resolve(binding, method, args);
        return Objects.requireNonNull(subjectMapper.map(id), "SubjectMapper returned null")
                .chain(subject -> directory.check(binding.objectType(), objectId, binding.relation(),
                        binding.subjectType(), subject)
                        .map(allowed -> {
                            if (!Boolean.TRUE.equals(allowed)) {
                                LOG.warnf("Topaz denied %s: %s lacks %s on %s", what,
                                        binding.subjectType(), binding.relation(), binding.objectType());
                                LOG.debugf("Topaz denied %s: %s:%s lacks %s on %s:%s", what,
                                        binding.subjectType(), subject, binding.relation(), binding.objectType(),
                                        objectId);
                                throw new ForbiddenException();
                            }
                            return null;
                        }));
    }
}
