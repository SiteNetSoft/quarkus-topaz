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

    @Test
    void everyRuntimePackageWithPublicClassesIsMarkedInternal() throws Exception {
        java.nio.file.Path classes = java.nio.file.Path.of(TopazClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        java.nio.file.Path root = classes.resolve("org/sitenetsoft/quarkus/topaz/runtime");
        try (java.util.stream.Stream<java.nio.file.Path> dirs = java.nio.file.Files.walk(root)) {
            for (java.nio.file.Path dir : (Iterable<java.nio.file.Path>) dirs.filter(java.nio.file.Files::isDirectory)::iterator) {
                boolean hasPublic = false;
                try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
                    for (java.nio.file.Path f : (Iterable<java.nio.file.Path>) files.filter(
                            x -> x.toString().endsWith(".class") && !x.getFileName().toString().equals("package-info.class")
                                    && !x.getFileName().toString().contains("$"))::iterator) {
                        String name = classes.relativize(f).toString()
                                .replace(java.io.File.separatorChar, '.');
                        name = name.substring(0, name.length() - ".class".length());
                        hasPublic |= Modifier.isPublic(Class.forName(name).getModifiers());
                    }
                }
                if (hasPublic) {
                    String pkg = classes.relativize(dir).toString()
                            .replace(java.io.File.separatorChar, '.');
                    assertThat(Class.forName(pkg + ".package-info").getPackage().isAnnotationPresent(Internal.class))
                            .as(pkg).isTrue();
                }
            }
        }
    }
}
