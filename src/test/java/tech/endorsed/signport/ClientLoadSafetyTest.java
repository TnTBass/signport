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
        String buildGradle = Files.readString(Path.of("build.gradle"));
        String neoForgeBuildGradle = Files.readString(Path.of("neoforge/build.gradle"));
        String fabricMetadata = Files.readString(Path.of("src/fabric/resources/fabric.mod.json"));
        String neoForgeMetadata = Files.readString(Path.of("src/neoforge/resources/META-INF/neoforge.mods.toml"));

        assertTrue(properties.contains("minecraft_version=26.2"));
        assertTrue(properties.contains("loader_version=0.19.3"));
        assertTrue(properties.contains("mod_version=2.3.0+mc26.2"));
        assertTrue(properties.contains("fabric_version=0.152.2+26.2"));
        assertTrue(properties.contains("neoforge_version=26.2.0.6-beta"));
        assertTrue(buildGradle.contains("publicModVersion"));
        assertTrue(buildGradle.contains("project.version.toString().split(\"\\\\+\")[0]"));
        assertTrue(neoForgeBuildGradle.contains("publicModVersion"));
        assertTrue(neoForgeBuildGradle.contains("rootProject.version.toString().split(\"\\\\+\")[0]"));
        assertTrue(fabricMetadata.contains("\"version\": \"${displayVersion}\""));
        assertFalse(fabricMetadata.contains("\"version\": \"${version}\""));
        assertTrue(neoForgeMetadata.contains("version=\"${displayVersion}\""));
        assertFalse(neoForgeMetadata.contains("version=\"${version}\""));
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
        assertTrue(config.contains("Enable anchor browser keybind"));
        assertTrue(fabricClient.contains("ScreenNavigation.install(screen -> Minecraft.getInstance().setScreenAndShow(screen))"));
        assertTrue(neoForgeClient.contains("ScreenNavigation.install(screen -> Minecraft.getInstance().setScreenAndShow(screen))"));
        assertFalse(fabricClient.contains("client.screen"));
        assertTrue(fabricClient.contains("Keybind entrypoints open from gameplay"));
        assertTrue(neoForgeClient.contains("RegisterKeyMappingsEvent"));
        assertTrue(neoForgeClient.contains("event.register(configKey)"));
        assertTrue(neoForgeClient.contains("event.register(browserKey)"));
        assertFalse(fabricClient.contains("if (SignPortClientConfig.get().browserKeybindEnabled) {\n            browserKey = KeyMappingHelper.registerKeyMapping"));
        assertFalse(neoForgeClient.contains("if (SignPortClientConfig.get().browserKeybindEnabled) {\n            browserKey = new KeyMapping"));
        assertTrue(neoForgeClient.contains("while (configKey.consumeClick())"));
        assertTrue(neoForgeClient.contains("while (browserKey.consumeClick())"));
        assertTrue(fabricClient.contains("SignPortClientConfig.get().browserKeybindEnabled"));
        assertTrue(neoForgeClient.contains("SignPortClientConfig.get().browserKeybindEnabled"));
        assertTrue(fabricClient.contains("PortSignHudHint.tick(client)"));
        assertTrue(neoForgeClient.contains("PortSignHudHint.tick(client)"));
        assertTrue(neoForgeClient.contains("openConfigScreen(client)"));
        assertTrue(neoForgeClient.contains("openAnchorBrowser(client)"));
        assertTrue(neoForgeClient.contains("IConfigScreenFactory"));
        assertTrue(neoForgeClient.contains("ModLoadingContext.get().registerExtensionPoint"));
        assertTrue(neoForgeClient.contains("ConfigScreenFactory.create(parent)"));
        assertTrue(modMenu.contains("ConfigScreenFactory.create(parent)"));
        assertTrue(fabricClient.contains("AnchorBrowserScreen.active() instanceof AnchorBrowserScreen browser"));
        assertTrue(neoForgeClient.contains("AnchorBrowserScreen.active() instanceof AnchorBrowserScreen browser"));
    }

    @Test
    void signTemplateDialogExplainsGeneratedSignFormat() throws IOException {
        String signEditorMixin = Files.readString(Path.of(
                "src/commonClient/java/tech/endorsed/signport/mixin/AbstractSignEditScreenMixin.java"));

        assertFalse(signEditorMixin.contains("Example sign"));
        assertTrue(signEditorMixin.contains("Line 1: Any label (ignored)"));
        assertTrue(signEditorMixin.contains("Line 2: [sp]"));
        assertTrue(signEditorMixin.contains("Line 3: Anchor name"));
        assertTrue(signEditorMixin.contains("Line 4: Dimension or blank"));
        assertTrue(signEditorMixin.contains("TEMPLATE_HEIGHT = 224"));
        assertTrue(signEditorMixin.contains("signportTemplateTop + 78"));
        assertTrue(signEditorMixin.contains("signportTemplateTop + 126"));
        assertTrue(signEditorMixin.contains("signportHandleTemplateChar"));
        assertTrue(signEditorMixin.contains("signportTemplateTargetField.charTyped(event)"));
        assertTrue(signEditorMixin.contains("signportTemplateLabelField.charTyped(event)"));
        assertTrue(signEditorMixin.contains("signportTemplateTargetField.keyPressed(event)"));
        assertTrue(signEditorMixin.contains("signportTemplateLabelField.keyPressed(event)"));
        assertTrue(signEditorMixin.contains("focusTemplateField(signportTemplateLabelField)"));
        assertTrue(signEditorMixin.contains("event.key() == 257 || event.key() == 335"));
        assertTrue(signEditorMixin.contains("renderTemplateControls"));
        assertTrue(signEditorMixin.contains("signportTemplateTargetField.extractRenderState"));
        assertTrue(signEditorMixin.indexOf("renderTemplateControls(graphics, mouseX, mouseY)")
                > signEditorMixin.indexOf("graphics.fill(signportTemplateLeft, signportTemplateTop"));
        assertTrue(signEditorMixin.indexOf("renderTemplateExample(graphics)")
                > signEditorMixin.indexOf("renderTemplateControls(graphics, mouseX, mouseY)"));
        assertTrue(signEditorMixin.contains("renderTemplateSignPreview"));
        assertTrue(signEditorMixin.contains("\"Home\""));
        assertTrue(signEditorMixin.contains("\"[signport]\""));
        assertTrue(signEditorMixin.contains("\"spawn\""));
        assertTrue(signEditorMixin.contains("\"overworld\""));
        assertTrue(signEditorMixin.contains("selectedTemplateDimension().map(option -> dimensionShortName(option.dimension())).orElse(\"\")"));
        assertTrue(signEditorMixin.contains("drawCenteredSignText"));
        assertTrue(signEditorMixin.contains("font.width(text)"));
        assertTrue(signEditorMixin.contains("0xFFFFF4C8"));
        assertFalse(signEditorMixin.contains("0xFF24170D"));
        assertFalse(signEditorMixin.contains("\"minecraft:the_nether\""));
        assertTrue(signEditorMixin.contains("renderTemplateHelpTooltip"));
        assertTrue(signEditorMixin.contains("hoverTemplateExampleLine"));
        assertFalse(signEditorMixin.contains("hoverTemplateLabel"));
        assertTrue(signEditorMixin.contains("Line 1 is arbitrary display text."));
        assertTrue(signEditorMixin.contains("It does not affect portal creation."));
        assertTrue(signEditorMixin.contains("Target is the anchor name for line 3."));
        assertTrue(signEditorMixin.contains("Enter a dimension when the anchor is in a different dimension."));
    }

    @Test
    void clientTranslationsAreSharedAcrossLoaders() throws IOException {
        String fabricBuild = Files.readString(Path.of("build.gradle"));
        String neoForgeBuild = Files.readString(Path.of("neoforge/build.gradle"));
        Path commonLangPath = Path.of("src/commonClient/resources/assets/signport/lang/en_us.json");
        String commonLang = Files.readString(commonLangPath);

        assertTrue(fabricBuild.contains("resources.setSrcDirs([\"src/commonClient/resources\", \"src/fabricClient/resources\"])"));
        assertTrue(neoForgeBuild.contains("resources.setSrcDirs([\"../src/common/resources\", \"../src/commonClient/resources\", \"../src/neoforge/resources\"])"));
        assertFalse(Files.exists(Path.of("src/fabricClient/resources/assets/signport/lang/en_us.json")));
        assertTrue(commonLang.contains("\"key.category.signport.signport\": \"SignPort\""));
        assertFalse(commonLang.contains("\"category.signport.signport\""));
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
