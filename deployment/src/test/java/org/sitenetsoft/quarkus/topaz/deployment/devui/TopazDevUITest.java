package org.sitenetsoft.quarkus.topaz.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import org.jboss.shrinkwrap.api.asset.StringAsset;

public class TopazDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource("topaz")
                    .addAsResource(new StringAsset("""
                            quarkus.topaz.devservices.policy.directory=classpath:topaz/policies
                            quarkus.topaz.devservices.manifest-location=classpath:topaz/manifest.yaml
                            quarkus.topaz.devservices.data-location=classpath:topaz/data
                            """), "application.properties"));

    public TopazDevUITest() {
        super("org.sitenetsoft.quarkus-topaz");
    }

    @Test
    void listPoliciesReturnsTodoPackages() throws Exception {
        JsonNode policies = executeJsonRPCMethod("listPolicies");
        assertThat(policies.isArray()).isTrue();
        boolean found = false;
        for (JsonNode p : policies) {
            found |= p.get("packagePath").asText().matches("(data\\.)?todoApp\\..*");
        }
        assertThat(found).as("policies: %s", policies).isTrue();
    }

    @Test
    void manifestReturned() throws Exception {
        assertThat(executeJsonRPCMethod("getManifest").asText()).contains("can_delete");
    }

    @Test
    void connectionDescribesEndpoints() throws Exception {
        JsonNode c = executeJsonRPCMethod("connection");
        assertThat(c.get("authorizer").asText()).contains(":");
        assertThat(c.get("directory").asText()).contains(":");
    }
}
