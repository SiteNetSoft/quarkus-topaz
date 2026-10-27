package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import org.sitenetsoft.quarkus.topaz.zanzibar.ZanzibarTopazRelationshipManager;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Registers the Topaz relationship manager of the Zanzibar connector at build time.
 */
class TopazZanzibarProcessor {

    /** The feature name. */
    static final String FEATURE = "topaz-zanzibar";

    /**
     * Create the processor.
     */
    TopazZanzibarProcessor() {
    }

    /**
     * Register the feature name.
     *
     * @return the feature build item
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register the relationship manager as an unremovable bean.
     *
     * @return the additional bean build item
     */
    @BuildStep
    AdditionalBeanBuildItem relationshipManager() {
        return AdditionalBeanBuildItem.unremovableOf(ZanzibarTopazRelationshipManager.class);
    }
}
