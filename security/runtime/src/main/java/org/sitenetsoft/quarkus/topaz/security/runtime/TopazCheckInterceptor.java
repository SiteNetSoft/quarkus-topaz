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
 * Enforces {@link TopazCheck}.
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

    @Inject
    TopazCheckInterceptor(InterceptorSupport support, DirectoryClient directory, SubjectMapper subjectMapper,
            TopazEnforcementConfig config, MapperDispatch dispatch) {
        this.support = support;
        this.directory = directory;
        this.subjectMapper = subjectMapper;
        this.config = config;
        this.dispatch = dispatch;
    }

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        TopazCheck binding = ctx.getInterceptorBinding(TopazCheck.class);
        Uni<SecurityIdentity> identity = support.identity();
        Object[] args = ctx.getParameters();
        Method method = ctx.getMethod();
        String what = method.getDeclaringClass().getName() + "#" + method.getName();
        boolean callerMayBlock = InterceptorSupport.blockingDispatch(method);
        // failClosed covers the identity lookup and the object id resolution too: any failure denies
        Uni<Void> check = InterceptorSupport.failClosed(identity.chain(id -> {
            if (id.isAnonymous()) {
                return Uni.createFrom().failure(new UnauthorizedException());
            }
            return dispatch.run(() -> check(binding, id, method, args, what), MapperDispatch.Point.CHECK,
                    callerMayBlock);
        }), what, config.timeout());
        return InterceptorSupport.dispatch(ctx, check, config.timeout());
    }

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
