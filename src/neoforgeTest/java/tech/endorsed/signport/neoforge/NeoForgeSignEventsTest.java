package tech.endorsed.signport.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeSignEventsTest {
    @Test
    void neoForgeEntrypointRegistersSignEventHandlers() throws IOException {
        String entrypoint = Files.readString(repoPath(
                "src/neoforge/java/tech/endorsed/signport/neoforge/SignPortNeoForge.java"));

        assertTrue(entrypoint.contains("NeoForgeSignEvents.register();"));
    }

    @Test
    void signEventAdapterDelegatesToCommonSignAndPermissionBehavior() throws IOException {
        String adapter = Files.readString(repoPath(
                "src/neoforge/java/tech/endorsed/signport/neoforge/events/NeoForgeSignEvents.java"));

        assertTrue(adapter.contains("BreakBlockEvent"));
        assertTrue(adapter.contains("UseItemOnBlockEvent"));
        assertTrue(adapter.contains("event.getHand() != InteractionHand.MAIN_HAND"));
        assertTrue(adapter.contains("SignPortPermissions.canBreakSign(player)"));
        assertTrue(adapter.contains("if (player == null) return;"));
        assertTrue(adapter.contains("SignPortPermissions.canUseSign(player)"));
        assertTrue(adapter.contains("SignPortPermissions.canEditSign(player)"));
        assertTrue(adapter.contains("PortSignEntity.isSignPortSign(front)"));
        assertTrue(adapter.contains("PortSignEntity.isSignPortSign(back)"));
        assertTrue(adapter.contains("PortSignEntity.resolvePortalDestination(level, primaryText, secondaryText)"));
        assertTrue(adapter.contains("PortSignEntity.teleportToDestination(player, destination, primaryText, secondaryText)"));
        assertTrue(adapter.contains("sign.isFacingFrontText(player)"));
        assertTrue(adapter.contains("event.cancelWithResult(InteractionResult.FAIL)"));
        assertTrue(adapter.contains("event.cancelWithResult(InteractionResult.SUCCESS)"));
        assertInOrder(
                adapter,
                "SignPortPermissions.canEditSign(player) && player.hasPose(Pose.CROUCHING)",
                "PortSignEntity.teleportToDestination(player, destination, primaryText, secondaryText)");
    }

    @Test
    void neoForgeMetadataActivatesCommonSignCreateEditMixin() throws IOException {
        String metadata = Files.readString(repoPath("src/neoforge/resources/META-INF/neoforge.mods.toml"));
        String mixins = Files.readString(repoPath("src/neoforge/resources/signport.neoforge.mixins.json"));
        String fabricMixins = Files.readString(repoPath("src/fabric/resources/signport.mixins.json"));

        assertTrue(metadata.contains("[[mixins]]"));
        assertTrue(metadata.contains("config=\"signport.neoforge.mixins.json\""));
        assertTrue(mixins.contains("\"minVersion\": \"0.8\""));
        assertTrue(mixins.contains("\"package\": \"tech.endorsed.signport.mixin\""));
        assertTrue(mixins.contains("\"SignBlockEntityMixin\""));
        assertTrue(mixins.contains("\"compatibilityLevel\": \"" + compatibilityLevel(fabricMixins) + "\""));
    }

    private static String compatibilityLevel(String mixinConfig) {
        Matcher matcher = Pattern.compile("\"compatibilityLevel\"\\s*:\\s*\"([^\"]+)\"").matcher(mixinConfig);
        if (!matcher.find()) {
            throw new AssertionError("Missing compatibilityLevel in mixin config.");
        }
        return matcher.group(1);
    }

    private static void assertInOrder(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);

        assertTrue(firstIndex >= 0, "Expected source to contain " + first);
        assertTrue(secondIndex > firstIndex, "Expected " + second + " after " + first);
    }

    private static Path repoPath(String path) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path fromCurrent = current.resolve(path).normalize();
        if (Files.exists(fromCurrent)) {
            return fromCurrent;
        }

        Path fromSubproject = current.resolve("..").resolve(path).normalize();
        if (Files.exists(fromSubproject)) {
            return fromSubproject;
        }

        throw new AssertionError("Expected test fixture at " + path + " from working directory " + current);
    }
}
