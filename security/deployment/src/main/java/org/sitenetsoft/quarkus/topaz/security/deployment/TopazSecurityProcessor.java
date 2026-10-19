package org.sitenetsoft.quarkus.topaz.security.deployment;

import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultIdentityMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultPolicyPathMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultSubjectMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopDecisionCache;
import org.sitenetsoft.quarkus.topaz.security.runtime.NoopResourceMethodInfoResolver;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

class TopazSecurityProcessor {

    static final String FEATURE = "topaz-security";

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
}
