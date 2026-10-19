package org.sitenetsoft.quarkus.topaz.security.deployment;

import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.sitenetsoft.quarkus.topaz.security.TopazAuthorize;
import org.sitenetsoft.quarkus.topaz.security.TopazObjectId;
import org.sitenetsoft.quarkus.topaz.security.TopazResource;
import org.sitenetsoft.quarkus.topaz.security.runtime.DecisionEvaluator;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultIdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultPolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultSubjectMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.InterceptorSupport;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopDecisionCache;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopResourceMethodInfoResolver;
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazAuthorizeInterceptor;
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazCheckInterceptor;
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
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;

class TopazSecurityProcessor {

    static final String FEATURE = "topaz-security";

    private static final DotName AUTHORIZATION_POLICY = DotName
            .createSimple("io.quarkus.vertx.http.security.AuthorizationPolicy");

    private static final DotName TOPAZ_AUTHORIZE = DotName.createSimple(TopazAuthorize.class);

    private static final DotName TOPAZ_RESOURCE = DotName.createSimple(TopazResource.class);

    private static final DotName TOPAZ_OBJECT_ID = DotName.createSimple(TopazObjectId.class);

    private static final DotName JAXRS_PATH = DotName.createSimple("jakarta.ws.rs.Path");

    private static final DotName INTERCEPTOR = DotName.createSimple("jakarta.interceptor.Interceptor");

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

    @BuildStep
    AdditionalBeanBuildItem interceptorBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(TopazAuthorizeInterceptor.class, TopazCheckInterceptor.class, InterceptorSupport.class)
                .setUnremovable()
                .build();
    }

    // @TopazResource arguments are converted with Jackson, which reads their properties reflectively.
    @BuildStep
    void topazResourceReflection(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflective) {
        Set<String> types = new TreeSet<>();
        for (AnnotationInstance a : index.getIndex().getAnnotations(TOPAZ_RESOURCE)) {
            if (a.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                continue;
            }
            MethodParameterInfo parameter = a.target().asMethodParameter();
            Type type = parameter.method().parameterType(parameter.position());
            if (type.kind() == Type.Kind.CLASS || type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                String name = type.name().toString();
                if (!isJdkType(name)) {
                    types.add(name);
                }
            }
        }
        if (!types.isEmpty()) {
            reflective.produce(ReflectiveClassBuildItem.builder(types.toArray(String[]::new)).methods().fields()
                    .build());
        }
    }

    // @TopazCheck reads @TopazObjectId fields and getters of its arguments reflectively.
    @BuildStep
    void topazObjectIdReflection(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflective) {
        Set<String> types = new TreeSet<>();
        for (AnnotationInstance a : index.getIndex().getAnnotations(TOPAZ_OBJECT_ID)) {
            switch (a.target().kind()) {
                case FIELD -> types.add(a.target().asField().declaringClass().name().toString());
                case METHOD -> types.add(a.target().asMethod().declaringClass().name().toString());
                default -> {
                }
            }
        }
        if (!types.isEmpty()) {
            reflective.produce(ReflectiveClassBuildItem.builder(types.toArray(String[]::new)).methods().fields()
                    .build());
        }
    }

    private static boolean isJdkType(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.");
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
        boolean derivedPath = derivedTopazAuthorizePath(index.getIndex());
        recorder.validate(annotationUsesTopazPolicy(index) || derivedPath);
    }

    // A @TopazAuthorize without a path derives it from the matched JAX-RS resource method, so it must sit on one.
    private static boolean derivedTopazAuthorizePath(IndexView index) {
        boolean derived = false;
        for (AnnotationInstance a : index.getAnnotations(TOPAZ_AUTHORIZE)) {
            AnnotationValue path = a.value("path");
            if (path != null && !path.asString().isEmpty()) {
                continue;
            }
            ClassInfo declaring = switch (a.target().kind()) {
                case METHOD -> a.target().asMethod().declaringClass();
                case CLASS -> a.target().asClass();
                default -> null;
            };
            // the binding on an interceptor class selects the interceptor, it does not secure anything
            if (declaring == null || declaring.hasDeclaredAnnotation(INTERCEPTOR)) {
                continue;
            }
            if (!isJaxrsResource(index, declaring)) {
                throw new DeploymentException("@TopazAuthorize without a path on " + a.target()
                        + " in " + declaring.name() + ": the policy path can only be derived on a JAX-RS resource"
                        + " (a class annotated @Path); set path explicitly");
            }
            derived = true;
        }
        return derived;
    }

    private static boolean isJaxrsResource(IndexView index, ClassInfo type) {
        for (ClassInfo c = type; c != null; c = c.superName() == null ? null : index.getClassByName(c.superName())) {
            if (c.hasDeclaredAnnotation(JAXRS_PATH)) {
                return true;
            }
            if (c.enclosingClass() != null) {
                ClassInfo enclosing = index.getClassByName(c.enclosingClass());
                if (enclosing != null && enclosing.hasDeclaredAnnotation(JAXRS_PATH)) {
                    return true;
                }
            }
        }
        return false;
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
