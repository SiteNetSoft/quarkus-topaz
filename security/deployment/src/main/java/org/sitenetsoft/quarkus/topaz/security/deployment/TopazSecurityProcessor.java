package org.sitenetsoft.quarkus.topaz.security.deployment;

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
}
