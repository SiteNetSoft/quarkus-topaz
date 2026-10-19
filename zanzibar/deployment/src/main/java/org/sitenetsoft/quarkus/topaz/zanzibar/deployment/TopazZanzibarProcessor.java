package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import org.sitenetsoft.quarkus.topaz.zanzibar.ZanzibarTopazRelationshipManager;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class TopazZanzibarProcessor {

    static final String FEATURE = "topaz-zanzibar";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem relationshipManager() {
        return AdditionalBeanBuildItem.unremovableOf(ZanzibarTopazRelationshipManager.class);
    }
}
