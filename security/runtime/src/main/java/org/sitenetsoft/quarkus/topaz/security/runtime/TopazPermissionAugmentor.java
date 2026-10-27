package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.security.Permission;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.sitenetsoft.quarkus.topaz.DirectoryClient;
import org.sitenetsoft.quarkus.topaz.security.IdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.SubjectMapper;
import org.sitenetsoft.quarkus.topaz.security.TopazRelation;

import com.google.protobuf.Struct;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * The augmentor that bridges {@code @PermissionsAllowed} to Topaz.
 * <p>
 * It is installed only when {@code quarkus.topaz.enforcement.permissions.enabled=true}.
 * <p>
 * It adds a permission checker to every authenticated identity and leaves anonymous identities unchanged. The checker
 * checks a {@link TopazRelation} against the directory. It evaluates any other permission as the policy path named by
 * {@link Permission#getName()}, with an empty resource context.
 * <p>
 * A permission with actions is not supported and is denied. An example is
 * {@code @PermissionsAllowed("todoApp.GET.todos:write")}.
 * <p>
 * The checker fails closed. An error, a timeout set by {@code quarkus.topaz.enforcement.timeout} or a mapper failure
 * yields {@code false}.
 */
@ApplicationScoped
public class TopazPermissionAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = Logger.getLogger(TopazPermissionAugmentor.class);

    private final DecisionEvaluator evaluator;
    private final DirectoryClient directory;
    private final IdentityMapper identityMapper;
    private final SubjectMapper subjectMapper;
    private final TopazEnforcementConfig config;
    private final MapperDispatch dispatch;

    /**
     * Create the augmentor.
     *
     * @param evaluator the decision evaluator
     * @param directory the directory client
     * @param identityMapper the identity mapper
     * @param subjectMapper the subject mapper
     * @param config the enforcement configuration
     * @param dispatch the mapper dispatcher
     */
    @Inject
    TopazPermissionAugmentor(DecisionEvaluator evaluator, DirectoryClient directory, IdentityMapper identityMapper,
            SubjectMapper subjectMapper, TopazEnforcementConfig config, MapperDispatch dispatch) {
        this.evaluator = evaluator;
        this.directory = directory;
        this.identityMapper = identityMapper;
        this.subjectMapper = subjectMapper;
        this.config = config;
        this.dispatch = dispatch;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder(identity)
                .addPermissionChecker(permission -> check(identity, permission))
                .build());
    }

    /**
     * Check one permission for an identity.
     *
     * @param identity the identity
     * @param permission the permission
     * @return {@code true} when Topaz permits, {@code false} on a denial, an error or a timeout
     */
    private Uni<Boolean> check(SecurityIdentity identity, Permission permission) {
        Uni<Boolean> check;
        if (permission instanceof TopazRelation relation) {
            check = dispatch.run(() -> relation(identity, relation), MapperDispatch.Point.BRIDGE_RELATION, false)
                    .onFailure().recoverWithItem(e -> {
                        LOG.warnf("Topaz relation check failed for %s: %s", relation, e.toString());
                        return false;
                    });
        } else if (permission.getActions() != null && !permission.getActions().isEmpty()) {
            // A policy path has no notion of actions, and ignoring them would grant more than was asked for.
            LOG.warnf("Topaz denied %s with actions '%s': actions are not supported by the Topaz bridge",
                    permission.getName(), permission.getActions());
            return Uni.createFrom().item(false);
        } else {
            // The evaluator never fails and logs its own denials.
            check = dispatch.run(() -> Objects.requireNonNull(
                    identityMapper.map(identity, Optional.empty()), "IdentityMapper returned null")
                    .chain(id -> evaluator.evaluate(id, permission.getName(), Struct.getDefaultInstance()))
                    .map(DecisionEvaluator.Outcome::permitted), MapperDispatch.Point.BRIDGE_DECISION, false)
                    .onFailure().recoverWithItem(e -> {
                        LOG.warnf("Topaz denied %s for authenticated: %s", permission.getName(), e.toString());
                        LOG.debugf("Topaz denied %s for authenticated %s", permission.getName(),
                                identity.getPrincipal().getName());
                        return false;
                    });
        }
        return check
                .ifNoItem().after(config.timeout()).recoverWithItem(() -> {
                    LOG.warnf("Topaz permission check timed out for %s", permission);
                    return false;
                });
    }

    /**
     * Check a relation permission against the directory.
     *
     * @param identity the identity
     * @param relation the relation permission
     * @return {@code true} when the directory reports the relation
     */
    private Uni<Boolean> relation(SecurityIdentity identity, TopazRelation relation) {
        return Objects.requireNonNull(subjectMapper.map(identity), "SubjectMapper returned null")
                .chain(subject -> directory.check(relation.objectType(), relation.objectId(), relation.relation(),
                        relation.subjectType(), subject))
                .map(Boolean.TRUE::equals);
    }
}
