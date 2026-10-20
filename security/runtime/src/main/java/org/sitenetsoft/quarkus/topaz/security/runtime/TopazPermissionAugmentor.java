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
 * Bridges {@code @PermissionsAllowed} to Topaz; installed only when
 * {@code quarkus.topaz.enforcement.permissions.enabled=true}.
 * <p>
 * Adds a permission checker to every authenticated identity (anonymous identities are left unchanged). A
 * {@link TopazRelation} is checked against the directory; any other permission is evaluated as the policy path named by
 * {@link Permission#getName()} with an empty resource context; a permission with actions (for example
 * {@code @PermissionsAllowed("todoApp.GET.todos:write")}) is not supported and is denied. Fails closed: an error, a
 * timeout ({@code quarkus.topaz.enforcement.timeout}) or a mapper failure yields {@code false}.
 */
@ApplicationScoped
public class TopazPermissionAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = Logger.getLogger(TopazPermissionAugmentor.class);

    private final DecisionEvaluator evaluator;
    private final DirectoryClient directory;
    private final IdentityMapper identityMapper;
    private final SubjectMapper subjectMapper;
    private final TopazEnforcementConfig config;

    @Inject
    TopazPermissionAugmentor(DecisionEvaluator evaluator, DirectoryClient directory, IdentityMapper identityMapper,
            SubjectMapper subjectMapper, TopazEnforcementConfig config) {
        this.evaluator = evaluator;
        this.directory = directory;
        this.identityMapper = identityMapper;
        this.subjectMapper = subjectMapper;
        this.config = config;
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

    private Uni<Boolean> check(SecurityIdentity identity, Permission permission) {
        Uni<Boolean> check;
        if (permission instanceof TopazRelation relation) {
            check = Uni.createFrom().deferred(() -> relation(identity, relation))
                    .onFailure().recoverWithItem(e -> {
                        LOG.warnf("Topaz relation check failed for %s: %s", relation, e.toString());
                        return false;
                    });
        } else if (permission.getActions() != null && !permission.getActions().isEmpty()) {
            // a policy path has no notion of actions; ignoring them would grant more than was asked for
            LOG.warnf("Topaz denied %s with actions '%s': actions are not supported by the Topaz bridge",
                    permission.getName(), permission.getActions());
            return Uni.createFrom().item(false);
        } else {
            // the evaluator never fails and logs its own denials
            check = Uni.createFrom().deferred(() -> Objects.requireNonNull(
                    identityMapper.map(identity, Optional.empty()), "IdentityMapper returned null"))
                    .chain(id -> evaluator.evaluate(id, permission.getName(), Struct.getDefaultInstance()))
                    .map(DecisionEvaluator.Outcome::permitted)
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

    private Uni<Boolean> relation(SecurityIdentity identity, TopazRelation relation) {
        return Objects.requireNonNull(subjectMapper.map(identity), "SubjectMapper returned null")
                .chain(subject -> directory.check(relation.objectType(), relation.objectId(), relation.relation(),
                        relation.subjectType(), subject))
                .map(Boolean.TRUE::equals);
    }
}
