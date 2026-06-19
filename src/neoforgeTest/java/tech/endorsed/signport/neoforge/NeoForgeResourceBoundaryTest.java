package tech.endorsed.signport.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeResourceBoundaryTest {
    @Test
    void sharedAssetsAndFabricClientTranslationsStayInTheRightResourceRoots() throws IOException {
        String fabricBuild = Files.readString(repoPath("build.gradle"));
        String neoForgeBuild = Files.readString(repoPath("neoforge/build.gradle"));

        assertAll(
                () -> assertTrue(Files.exists(repoRoot().resolve("src/common/resources/assets/signport/icon.png"))),
                () -> assertFalse(Files.exists(repoRoot().resolve("src/common/resources/assets/signport/lang/en_us.json"))),
                () -> assertFalse(Files.exists(repoRoot().resolve("src/fabric/resources/assets/signport/icon.png"))),
                () -> assertFalse(Files.exists(repoRoot().resolve("src/fabric/resources/assets/signport/lang/en_us.json"))),
                () -> assertTrue(Files.exists(repoRoot().resolve("src/fabricClient/resources/assets/signport/lang/en_us.json"))),
                // These are source-contract checks for the multiloader layout convention.
                // Keep them exact so resource-root ownership changes are reviewed deliberately.
                () -> assertTrue(fabricBuild.contains("resources.setSrcDirs([\"src/common/resources\", \"src/fabric/resources\"])")),
                () -> assertTrue(neoForgeBuild.contains("resources.setSrcDirs([\"../src/common/resources\", \"../src/neoforge/resources\"])")),
                () -> assertTrue(neoForgeBuild.contains("exclude \"assets/**\"")),
                () -> assertTrue(neoForgeBuild.contains("exclude \"fabric.mod.json\"")),
                () -> assertTrue(neoForgeBuild.contains("exclude \"signport.mixins.json\"")));
    }

    private static Path repoPath(String path) {
        Path resolved = repoRoot().resolve(path);
        if (Files.exists(resolved)) {
            return resolved;
        }

        throw new AssertionError("Expected test fixture at " + path + " from working directory "
                + Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize());
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(current.resolve("settings.gradle"))) {
            return current;
        }

        Path parent = current.resolve("..").normalize();
        if (Files.exists(parent.resolve("settings.gradle"))) {
            return parent;
        }

        throw new AssertionError("Could not locate repository root from working directory " + current);
    }
}
