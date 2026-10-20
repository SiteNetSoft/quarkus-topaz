package org.sitenetsoft.quarkus.topaz;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sitenetsoft.quarkus.topaz.runtime.Internal;

class PublicApiTest {

    @Test
    void onlyIntendedTypesArePublicInTheApiPackage() throws URISyntaxException, ClassNotFoundException {
        File dir = Path.of(TopazClient.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("org/sitenetsoft/quarkus/topaz").toFile();
        Set<String> publicTypes = Arrays.stream(dir.listFiles((d, n) -> n.endsWith(".class") && !n.contains("$")
                && !n.equals("package-info.class")))
                .map(f -> f.getName().substring(0, f.getName().length() - ".class".length()))
                .filter(n -> {
                    try {
                        return Modifier.isPublic(Class.forName("org.sitenetsoft.quarkus.topaz." + n).getModifiers());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .collect(Collectors.toSet());
        assertThat(publicTypes).containsExactlyInAnyOrder("TopazClient", "AuthorizerClient", "DirectoryClient",
                "BlockingTopazClient", "BlockingAuthorizerClient", "BlockingDirectoryClient", "IdentityContexts",
                "Structs", "ImportSummary");
    }

    @Test
    void runtimePackageIsMarkedInternal() throws ClassNotFoundException {
        assertThat(Class.forName("org.sitenetsoft.quarkus.topaz.runtime.package-info").getPackage()
                .isAnnotationPresent(Internal.class)).isTrue();
    }
}
