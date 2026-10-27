package org.sitenetsoft.quarkus.topaz.security.deployment;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
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
import org.sitenetsoft.quarkus.topaz.security.runtime.TopazPermissionAugmentor;
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

/**
 * The build steps of the Topaz security extension.
 */
class TopazSecurityProcessor {

    /** The feature name. */
    static final String FEATURE = "topaz-security";

    private static final DotName AUTHORIZATION_POLICY = DotName
            .createSimple("io.quarkus.vertx.http.security.AuthorizationPolicy");

    private static final DotName TOPAZ_AUTHORIZE = DotName.createSimple(TopazAuthorize.class);

    private static final DotName TOPAZ_RESOURCE = DotName.createSimple(TopazResource.class);

    private static final DotName TOPAZ_OBJECT_ID = DotName.createSimple(TopazObjectId.class);

    private static final DotName JAXRS_PATH = DotName.createSimple("jakarta.ws.rs.Path");

    private static final DotName JAXRS_HTTP_METHOD = DotName.createSimple("jakarta.ws.rs.HttpMethod");

    private static final Set<DotName> JAXRS_METHODS = Set.of(
            DotName.createSimple("jakarta.ws.rs.GET"), DotName.createSimple("jakarta.ws.rs.POST"),
            DotName.createSimple("jakarta.ws.rs.PUT"), DotName.createSimple("jakarta.ws.rs.DELETE"),
            DotName.createSimple("jakarta.ws.rs.PATCH"), DotName.createSimple("jakarta.ws.rs.HEAD"),
            DotName.createSimple("jakarta.ws.rs.OPTIONS"));

    private static final DotName INTERCEPTOR = DotName.createSimple("jakarta.interceptor.Interceptor");

    private static final String REST_RESOLVER = "org.sitenetsoft.quarkus.topaz.security.runtime.rest"
            + ".RestResourceMethodInfoResolver";

    /**
     * Create the processor.
     */
    TopazSecurityProcessor() {
    }

    /**
     * Register the feature.
     *
     * @return the feature item
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Index the runtime module so bean discovery finds its classes.
     *
     * @return the index dependency
     */
    @BuildStep
    IndexDependencyBuildItem indexRuntime() {
        return new IndexDependencyBuildItem("org.sitenetsoft", "quarkus-topaz-security");
    }

    /**
     * Register the default mappers, resolver and cache.
     *
     * @return the unremovable beans
     */
    @BuildStep
    AdditionalBeanBuildItem defaultBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DefaultIdentityMapper.class, DefaultPolicyPathMapper.class,
                        DefaultResourceContextMapper.class, DefaultSubjectMapper.class,
                        NoopResourceMethodInfoResolver.class, NoopDecisionCache.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register the Quarkus REST resolver, or exclude it when REST is absent.
     * <p>
     * The resolver is referenced by name, so this class never loads it. Loading it needs quarkus-rest on the classpath.
     * As a non-default bean, it replaces {@link NoopResourceMethodInfoResolver}. The runtime jar is indexed. Without
     * REST, the resolver must be excluded explicitly, or bean discovery would still pick it up.
     *
     * @param capabilities the capabilities of the application
     * @param beans the producer of additional beans
     * @param excluded the producer of excluded types
     */
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

    /**
     * Require the body handler when the request body is read.
     *
     * @param buildTimeConfig the build-time configuration
     * @param bodyHandler the producer of the body handler requirement
     */
    @BuildStep
    void bodyHandler(TopazEnforcementBuildTimeConfig buildTimeConfig,
            BuildProducer<RequireBodyHandlerBuildItem> bodyHandler) {
        if (buildTimeConfig.readBody()) {
            bodyHandler.produce(new RequireBodyHandlerBuildItem());
        }
    }

    /**
     * Register the {@code @PermissionsAllowed} bridge, or exclude it when disabled.
     * <p>
     * The bridge is opt-in. The runtime jar is indexed. When the bridge is disabled, the augmentor must be excluded
     * explicitly, or bean discovery would still install it.
     *
     * @param buildTimeConfig the build-time configuration
     * @param beans the producer of additional beans
     * @param excluded the producer of excluded types
     */
    @BuildStep
    void permissionAugmentor(TopazEnforcementBuildTimeConfig buildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ExcludedTypeBuildItem> excluded) {
        if (buildTimeConfig.permissions().enabled()) {
            beans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(TopazPermissionAugmentor.class)
                    .setUnremovable()
                    .build());
        } else {
            excluded.produce(new ExcludedTypeBuildItem(TopazPermissionAugmentor.class.getName()));
        }
    }

    /**
     * Register the decision evaluator and the HTTP security policy.
     *
     * @return the unremovable beans
     */
    @BuildStep
    AdditionalBeanBuildItem policyBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(DecisionEvaluator.class, TopazHttpSecurityPolicy.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register the interceptors and their support bean.
     *
     * @return the unremovable beans
     */
    @BuildStep
    AdditionalBeanBuildItem interceptorBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(TopazAuthorizeInterceptor.class, TopazCheckInterceptor.class, InterceptorSupport.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register {@code @TopazResource} argument types for reflection.
     * <p>
     * Jackson converts these arguments and reads their properties reflectively.
     *
     * @param index the combined index
     * @param reflective the producer of reflective classes
     */
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

    /**
     * Register the classes that declare {@code @TopazObjectId} for reflection.
     * <p>
     * {@code @TopazCheck} reads the annotated fields and getters of its arguments reflectively.
     *
     * @param index the combined index
     * @param reflective the producer of reflective classes
     */
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

    /**
     * Tell whether a class name belongs to the JDK.
     *
     * @param name the class name
     * @return {@code true} for a JDK class name
     */
    private static boolean isJdkType(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.");
    }

    /**
     * Keep every HTTP security policy bean.
     * <p>
     * Named policies are looked up among the {@code HttpSecurityPolicy} beans at runtime, so none may be removed.
     *
     * @return the unremovable bean item
     */
    @BuildStep
    UnremovableBeanBuildItem unremovablePolicies() {
        return UnremovableBeanBuildItem.beanTypes(HttpSecurityPolicy.class);
    }

    /**
     * Validate the enforcement configuration at startup.
     * <p>
     * The {@code VertxBuildItem} orders this step after the Topaz client beans are created.
     *
     * @param recorder the recorder
     * @param index the combined index
     * @param vertx the Vert.x build item that fixes the step order
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void validate(TopazSecurityRecorder recorder, CombinedIndexBuildItem index, VertxBuildItem vertx) {
        boolean derivedPath = derivedTopazAuthorizePath(index.getIndex());
        recorder.validate(annotationUsesTopazPolicy(index) || derivedPath);
    }

    /**
     * Check every {@code @TopazAuthorize} without a path.
     * <p>
     * Such an annotation derives the path from the HTTP request being served. It must therefore sit on a JAX-RS
     * resource class or on a resource method. On any other method, it would gate that method with the policy of
     * whatever endpoint the caller happens to be on.
     *
     * @param index the index
     * @return {@code true} when at least one annotation derives its path
     * @throws DeploymentException when such an annotation is not on a JAX-RS resource
     */
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
            // The binding on an interceptor class selects the interceptor and does not secure anything.
            if (declaring == null || declaring.hasDeclaredAnnotation(INTERCEPTOR)) {
                continue;
            }
            if (!isJaxrsResource(index, declaring)) {
                throw new DeploymentException("@TopazAuthorize without a path on " + a.target()
                        + " in " + declaring.name() + ": the policy path can only be derived on a JAX-RS resource"
                        + " (a class annotated @Path, or implementing an interface annotated @Path); set path explicitly");
            }
            if (a.target().kind() == AnnotationTarget.Kind.METHOD
                    && !isResourceMethod(index, declaring, a.target().asMethod(), new HashSet<>())) {
                MethodInfo method = a.target().asMethod();
                throw new DeploymentException("@TopazAuthorize without a path on method " + method.name()
                        + " of " + declaring.name() + ": the method is not a JAX-RS resource method (no HTTP method"
                        + " annotation such as @GET), so the policy path cannot be derived for it; set path"
                        + " explicitly");
            }
            derived = true;
        }
        return derived;
    }

    /**
     * Tell whether a method is a JAX-RS resource method.
     * <p>
     * JAX-RS inherits the HTTP method annotation from an overridden superclass or interface method.
     *
     * @param index the index
     * @param type the type to search
     * @param method the method
     * @param seen the types already visited
     * @return {@code true} when the method or an overridden method carries an HTTP method annotation
     */
    private static boolean isResourceMethod(IndexView index, ClassInfo type, MethodInfo method, Set<DotName> seen) {
        if (type == null || !seen.add(type.name())) {
            return false;
        }
        MethodInfo declared = type.method(method.name(), method.parameterTypes());
        if (declared != null && hasHttpMethod(index, declared)) {
            return true;
        }
        if (type.superName() != null && isResourceMethod(index, index.getClassByName(type.superName()), method, seen)) {
            return true;
        }
        for (DotName name : type.interfaceNames()) {
            if (isResourceMethod(index, index.getClassByName(name), method, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell whether a method carries an HTTP method annotation.
     *
     * @param index the index
     * @param method the method
     * @return {@code true} when the method carries one
     */
    private static boolean hasHttpMethod(IndexView index, MethodInfo method) {
        for (AnnotationInstance annotation : method.declaredAnnotations()) {
            DotName name = annotation.name();
            if (JAXRS_METHODS.contains(name)) {
                return true;
            }
            ClassInfo annotationClass = index.getClassByName(name);
            if (annotationClass != null && annotationClass.hasDeclaredAnnotation(JAXRS_HTTP_METHOD)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell whether a class is a JAX-RS resource.
     * <p>
     * A resource carries {@code @Path}, directly or on a superclass, an implemented interface or an enclosing class.
     *
     * @param index the index
     * @param type the class
     * @return {@code true} for a JAX-RS resource
     */
    private static boolean isJaxrsResource(IndexView index, ClassInfo type) {
        for (ClassInfo c = type; c != null; c = c.superName() == null ? null : index.getClassByName(c.superName())) {
            if (c.hasDeclaredAnnotation(JAXRS_PATH) || hasPathInterface(index, c, new HashSet<>())) {
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

    /**
     * Tell whether a class implements an interface annotated {@code @Path}.
     *
     * @param index the index
     * @param type the class or interface
     * @param seen the interfaces already visited
     * @return {@code true} when an interface in the hierarchy carries {@code @Path}
     */
    private static boolean hasPathInterface(IndexView index, ClassInfo type, Set<DotName> seen) {
        for (DotName name : type.interfaceNames()) {
            if (!seen.add(name)) {
                continue;
            }
            ClassInfo iface = index.getClassByName(name);
            if (iface != null
                    && (iface.hasDeclaredAnnotation(JAXRS_PATH) || hasPathInterface(index, iface, seen))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell whether an {@code @AuthorizationPolicy} names the {@code topaz} policy.
     *
     * @param index the combined index
     * @return {@code true} when one does
     */
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
