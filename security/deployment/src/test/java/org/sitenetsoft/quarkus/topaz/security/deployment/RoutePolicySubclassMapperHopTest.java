package org.sitenetsoft.quarkus.topaz.security.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sitenetsoft.quarkus.topaz.security.ResourceMethodInfo;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultResourceContextMapper;
import org.sitenetsoft.quarkus.topaz.security.runtime.DefaultSubjectMapper;

import com.google.protobuf.Struct;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class RoutePolicySubclassMapperHopTest {

    static final AtomicReference<String> THREAD = new AtomicReference<>();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(TodoTestResource.class, TestIdentities.class, TestIdentityProvider.class,
                            TestIdentityController.class, SubclassMapper.class)
                    .addAsResource("topaz", "topaz"))
            .overrideConfigKey("quarkus.http.auth.basic", "true")
            .overrideConfigKey("quarkus.topaz.devservices.policy.directory", "classpath:topaz/policies")
            .overrideConfigKey("quarkus.topaz.devservices.manifest-location", "classpath:topaz/manifest.yaml")
            .overrideConfigKey("quarkus.topaz.devservices.data-location", "classpath:topaz/data")
            .overrideConfigKey("quarkus.topaz.enforcement.policy-root", "todoApp")
            .overrideConfigKey("quarkus.http.auth.permission.todos.paths", "/todos*")
            .overrideConfigKey("quarkus.http.auth.permission.todos.policy", "topaz");

    @BeforeAll
    static void identities() {
        TestIdentities.register();
    }

    @Test
    void subclassOfDefaultMapperTakesTheHop() {
        given().auth().preemptive().basic("rick@the-citadel.com", "rick")
                .get("/todos").then().statusCode(200);
        assertThat(THREAD.get()).isNotNull().doesNotStartWith("vert.x-eventloop");
    }

    @Test
    void onlyTheBuiltInClassIsFreeOfBlocking() {
        assertThat(new DefaultSubjectMapper().blocking()).isFalse();
        assertThat(new DefaultSubjectMapper() {
        }.blocking()).isTrue();
    }

    @Singleton
    public static class SubclassMapper extends DefaultResourceContextMapper {

        @Override
        public Uni<Struct> map(RoutingContext request, Optional<ResourceMethodInfo> method) {
            THREAD.set(Thread.currentThread().getName());
            return super.map(request, method);
        }
    }
}
