package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class TopazZanzibarProcessor {

    static final String FEATURE = "topaz-zanzibar";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
