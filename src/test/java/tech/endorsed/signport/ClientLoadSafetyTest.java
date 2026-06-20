package tech.endorsed.signport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLoadSafetyTest {
    @Test
    void modMetadataLoadsExpectedEntrypointsOnly() throws IOException {
        String metadata = Files.readString(Path.of("src/fabric/resources/fabric.mod.json"));

        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("main", List.of("tech.endorsed.signport.fabric.SignPortFabric"));
        expected.put("client", List.of("tech.endorsed.signport.fabric.client.SignPortFabricClient"));
        expected.put("modmenu", List.of("tech.endorsed.signport.fabric.client.config.SignPortModMenuApi"));

        assertEquals(expected, parseEntrypoints(metadata),
                "fabric.mod.json entrypoints drift; update this test only when intentionally changing the loaded entrypoint set.");
    }

    @Test
    void mixinMetadataLoadsExactlyTheExpectedClientMixins() throws IOException {
        String metadata = Files.readString(Path.of("src/fabric/resources/signport.mixins.json"));

        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                "AbstractSignEditScreenMixin",
                "ScreenAccessor"));
        assertEquals(expected, parseStringArray(metadata, "client"),
                "Client mixin allow-list drift; update this test only when intentionally changing the loaded client mixin set.");
    }

    @Test
    void neoForgeMetadataDeclaresOnlyNeoForgeEntrypoints() throws IOException {
        String metadata = Files.readString(Path.of("src/neoforge/resources/META-INF/neoforge.mods.toml"));

        assertTrue(metadata.contains("modLoader=\"javafml\""));
        assertTrue(metadata.contains("modId=\"signport\""));
        assertFalse(metadata.contains("entrypoint="));
        assertFalse(metadata.contains("clientEntrypoint="));
        assertFalse(metadata.contains("tech.endorsed.signport.fabric"));
        assertFalse(Files.exists(Path.of("src/neoforge/resources/fabric.mod.json")));
    }

    @Test
    void neoForgeEntrypointsUseLoaderAnnotationAndCommonModId() throws IOException {
        String server = Files.readString(Path.of("src/neoforge/java/tech/endorsed/signport/neoforge/SignPortNeoForge.java"));
        String client = Files.readString(Path.of("src/neoforgeClient/java/tech/endorsed/signport/neoforge/client/SignPortNeoForgeClient.java"));

        assertTrue(server.contains("import net.neoforged.fml.common.Mod;"));
        assertTrue(server.contains("@Mod(SignPort.MOD_ID)"));
        assertTrue(server.contains("import tech.endorsed.signport.SignPort;"));
        assertFalse(server.contains("net.fabricmc"));

        assertTrue(client.contains("import net.neoforged.api.distmarker.Dist;"));
        assertTrue(client.contains("import net.neoforged.fml.common.Mod;"));
        assertTrue(client.contains("@Mod(value = SignPort.MOD_ID, dist = Dist.CLIENT)"));
        assertTrue(client.contains("import tech.endorsed.signport.SignPort;"));
        assertFalse(client.contains("net.fabricmc"));
    }

    @Test
    void metadataTargetsMinecraft26Point2WithLoaderFloors() throws IOException {
        String properties = Files.readString(Path.of("gradle.properties"));
        String fabricMetadata = Files.readString(Path.of("src/fabric/resources/fabric.mod.json"));
        String neoForgeMetadata = Files.readString(Path.of("src/neoforge/resources/META-INF/neoforge.mods.toml"));

        assertTrue(properties.contains("minecraft_version=26.2"));
        assertTrue(properties.contains("loader_version=0.19.3"));
        assertTrue(properties.contains("mod_version=2.2.3+mc26.2"));
        assertTrue(properties.contains("fabric_version=0.152.2+26.2"));
        assertTrue(properties.contains("neoforge_version=26.2.0.6-beta"));
        assertTrue(fabricMetadata.contains("\"fabricloader\": \">=0.19.3\""));
        assertTrue(fabricMetadata.contains("\"minecraft\": \">=26.2\""));
        assertTrue(neoForgeMetadata.contains("versionRange=\"[26.2.0.6-beta,)\""));
        assertTrue(neoForgeMetadata.contains("versionRange=\"[26.2,)\""));
        assertFalse(neoForgeMetadata.contains("versionRange=\"[26.2]\""));
    }

    @Test
    void screenNavigationUsesLoaderInstalledImplementation() throws IOException {
        String commonNavigation = Files.readString(Path.of("src/commonClient/java/tech/endorsed/signport/client/ScreenNavigation.java"));
        String browser = Files.readString(Path.of("src/commonClient/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java"));
        String config = Files.readString(Path.of("src/commonClient/java/tech/endorsed/signport/client/config/SignPortConfigScreen.java"));
        String fabricClient = Files.readString(Path.of("src/fabricClient/java/tech/endorsed/signport/fabric/client/SignPortFabricClient.java"));
        String modMenu = Files.readString(Path.of("src/fabricClient/java/tech/endorsed/signport/fabric/client/config/SignPortModMenuApi.java"));
        String neoForgeClient = Files.readString(Path.of("src/neoforgeClient/java/tech/endorsed/signport/neoforge/client/SignPortNeoForgeClient.java"));

        assertFalse(commonNavigation.contains("net.fabricmc"));
        assertFalse(commonNavigation.contains("net.neoforged"));
        assertFalse(commonNavigation.contains("MinecraftAccessor"));
        assertFalse(commonNavigation.contains("setScreenAndShow"));
        assertTrue(browser.contains("ScreenNavigation.show(parent)"));
        assertTrue(browser.contains("static AnchorBrowserScreen active()"));
        Matcher constructor = Pattern.compile("(?s)public AnchorBrowserScreen\\(Screen parent\\) \\{(.*?)\\R    \\}").matcher(browser);
        assertTrue(constructor.find());
        assertFalse(constructor.group(1).contains("active = this;"));
        Matcher init = Pattern.compile("(?s)protected void init\\(\\) \\{.*?active = this;").matcher(browser);
        assertTrue(init.find());
        assertTrue(config.contains("ScreenNavigation.show(parent)"));
        assertTrue(fabricClient.contains("ScreenNavigation.install(screen -> Minecraft.getInstance().setScreenAndShow(screen))"));
        assertTrue(neoForgeClient.contains("ScreenNavigation.install(screen -> Minecraft.getInstance().setScreenAndShow(screen))"));
        assertFalse(fabricClient.contains("client.screen"));
        assertTrue(fabricClient.contains("Keybind entrypoints open from gameplay"));
        assertTrue(modMenu.contains("ConfigScreenFactory.create(parent)"));
        assertTrue(fabricClient.contains("AnchorBrowserScreen.active() instanceof AnchorBrowserScreen browser"));
        assertTrue(neoForgeClient.contains("AnchorBrowserScreen.active() instanceof AnchorBrowserScreen browser"));
    }

    private static Map<String, List<String>> parseEntrypoints(String metadata) {
        Matcher outer = Pattern.compile("\"entrypoints\"\\s*:\\s*\\{(.*?)}", Pattern.DOTALL).matcher(metadata);
        if (!outer.find()) return Map.of();
        String body = outer.group(1);
        Map<String, List<String>> result = new LinkedHashMap<>();
        Matcher key = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(body);
        while (key.find()) {
            List<String> values = new java.util.ArrayList<>();
            Matcher v = Pattern.compile("\"([^\"]+)\"").matcher(key.group(2));
            while (v.find()) values.add(v.group(1));
            result.put(key.group(1), values);
        }
        return result;
    }

    private static Set<String> parseStringArray(String metadata, String key) {
        Matcher block = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(metadata);
        if (!block.find()) return Set.of();
        Set<String> entries = new LinkedHashSet<>();
        Matcher entry = Pattern.compile("\"([^\"]+)\"").matcher(block.group(1));
        while (entry.find()) entries.add(entry.group(1));
        return entries;
    }
}
