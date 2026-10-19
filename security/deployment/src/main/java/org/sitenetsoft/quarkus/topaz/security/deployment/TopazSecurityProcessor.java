package org.sitenetsoft.quarkus.topaz.security.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.sitenetsoft.quarkus.topaz.security.runtime.DecisionEvaluator;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultIdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultPolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultSubjectMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopDecisionCache;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopResourceMethodInfoResolver;
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazEnforcementBuildTimeConfig;
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazHttpSecurityPolicy;
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazSecurityRecorder;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;

class TopazSecurityProcessor {

    static final String FEATURE = "topaz-security";

    private static final DotName AUTHORIZATION_POLICY = DotName
            .createSimple("io.quarkus.vertx.http.security.AuthorizationPolicy");

    private static final String REST_RESOLVER = "org.sitenetsoft.quarkus.topaz.security.runtime.rest"
            + ".RestResourceMethodInfoResolver";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexRuntime() {
        return new IndexDependencyBuildItem("org.sitenetsoft", "quarkus-topaz-security");
    }

    @BuildStep
    AdditionalBeanBuildItem defaultBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultIdentityMapper.class, DefaultPolicyPathMapper.class,
                        DefaultResourceContextMapper.class, DefaultSubjectMapper.class,
                        NoopResourceMethodInfoResolver.class, NoopDecisionCache.class)
                .setUnremovable()
                .build();
    }

    // Referenced by name so this class never loads the resolver, which needs quarkus-rest on the classpath.
    // As a non-default bean it replaces NoopResourceMethodInfoResolver. The runtime jar is indexed, so without REST
    // the resolver must be excluded explicitly or bean discovery would still pick it up.
    @BuildStep
    void restResolver(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ExcludedTypeBuildItem> excluded) {
        if (capabilities.isPresent(Capability.REST)) {
            beans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(REST_RESOLVER)
                    .setUnremovable()
                    .build());
        } else {
            excluded.produce(new ExcludedTypeBuildItem(REST_RESOLVER));
        }
    }

    @BuildStep
    void bodyHandler(TopazEnforcementBuildTimeConfig buildTimeConfig,
            BuildProducer<RequireBodyHandlerBuildItem> bodyHandler) {
        if (buildTimeConfig.readBody()) {
            bodyHandler.produce(new RequireBodyHandlerBuildItem());
        }
    }

    @BuildStep
    AdditionalBeanBuildItem policyBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DecisionEvaluator.class, TopazHttpSecurityPolicy.class)
                .setUnremovable()
                .build();
    }

    // Named policies are looked up among the HttpSecurityPolicy beans at runtime, so none may be removed.
    @BuildStep
    UnremovableBeanBuildItem unremovablePolicies() {
        return UnremovableBeanBuildItem.beanTypes(HttpSecurityPolicy.class);
    }

    // VertxBuildItem orders this after the Topaz client beans are created.
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void validate(TopazSecurityRecorder recorder, CombinedIndexBuildItem index, VertxBuildItem vertx) {
        recorder.validate(annotationUsesTopazPolicy(index));
    }

    private static boolean annotationUsesTopazPolicy(CombinedIndexBuildItem index) {
        for (AnnotationInstance a : index.getIndex().getAnnotations(AUTHORIZATION_POLICY)) {
            AnnotationValue name = a.value("name");
            if (name != null && TopazHttpSecurityPolicy.NAME.equals(name.asString())) {
                return true;
            }
        }
        return false;
    }
}
