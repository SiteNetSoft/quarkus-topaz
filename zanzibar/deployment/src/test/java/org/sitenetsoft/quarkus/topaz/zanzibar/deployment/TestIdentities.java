package org.sitenetsoft.quarkus.topaz.zanzibar.deployment;

import io.quarkus.security.test.utils.TestIdentityController;

final class TestIdentities {

    private TestIdentities() {
    }

    static void register() {
        TestIdentityController.resetRoles()
                .add("rick@the-citadel.com", "rick")
                .add("morty@the-citadel.com", "morty")
                .add("summer@the-citadel.com", "summer");
    }
}
