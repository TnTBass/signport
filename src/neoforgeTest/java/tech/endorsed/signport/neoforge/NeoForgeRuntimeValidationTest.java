package tech.endorsed.signport.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeRuntimeValidationTest {
    @Test
    void rootBuildValidatesDeclaredNeoForgeRuntimeHarness() throws IOException {
        String build = Files.readString(repoPath("build.gradle"));
        String neoForgeBuild = Files.readString(repoPath("neoforge/build.gradle"));

        assertAll(
                () -> assertTrue(neoForgeBuild.contains("mods {")),
                () -> assertTrue(neoForgeBuild.contains("runs {")),
                () -> assertTrue(neoForgeBuild.contains("server {")),
                () -> assertTrue(neoForgeBuild.contains("server()")),
                () -> assertTrue(build.contains("checkNeoForgeRuntimeLaunchBoundary")),
                () -> assertTrue(build.contains("project(\":neoforge\").tasks.named(\"createLaunchScripts\")")),
                () -> assertTrue(build.contains("taskNames.contains(\"runServer\")")),
                () -> assertTrue(build.contains("runServer")),
                () -> assertTrue(build.contains("serverRunClasspath.txt")),
                () -> assertTrue(build.contains("-Dfml.modFolders=signport")),
                () -> assertTrue(build.contains("net.neoforged.devlaunch.Main")));
    }

    private static Path repoPath(String path) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path repoRoot = current;
        if (!Files.exists(repoRoot.resolve("settings.gradle"))) {
            repoRoot = current.resolve("..").normalize();
            if (!Files.exists(repoRoot.resolve("settings.gradle"))) {
                throw new AssertionError("Could not locate repository root from working directory " + current);
            }
        }
        Path resolved = repoRoot.resolve(path);
        if (Files.exists(resolved)) {
            return resolved;
        }

        throw new AssertionError("Expected test fixture at " + path + " from working directory " + current);
    }
}
