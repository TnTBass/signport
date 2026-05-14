package tech.endorsed.signport;

import net.fabricmc.api.EnvType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLoadSafetyTest {
    @Test
    void physicalClientDoesNotRunServerInitializerHooks() {
        assertFalse(SignPort.shouldRegisterServerHooks(EnvType.CLIENT));
        assertTrue(SignPort.shouldRegisterServerHooks(EnvType.SERVER));
    }

    @Test
    void modMetadataDoesNotLoadOptionalClientEntrypoints() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));

        assertFalse(metadata.contains("\"client\""));
        assertFalse(metadata.contains("\"modmenu\""));
        assertFalse(metadata.contains("tech.endorsed.signport.client"));
    }

    @Test
    void mixinMetadataLoadsExactlyTheExpectedClientMixins() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/signport.mixins.json"));

        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                "AbstractSignEditScreenMixin",
                "ScreenAccessor"));
        assertEquals(expected, parseClientMixins(metadata),
                "Client mixin allow-list drift; update this test only when intentionally changing the loaded client mixin set.");
    }

    private static Set<String> parseClientMixins(String metadata) {
        Matcher block = Pattern.compile("\"client\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(metadata);
        if (!block.find()) return Set.of();
        Set<String> entries = new LinkedHashSet<>();
        Matcher entry = Pattern.compile("\"([^\"]+)\"").matcher(block.group(1));
        while (entry.find()) entries.add(entry.group(1));
        return entries;
    }
}
