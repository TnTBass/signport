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

class ClientLoadSafetyTest {
    @Test
    void modMetadataLoadsExpectedEntrypointsOnly() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));

        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("main", List.of("tech.endorsed.signport.SignPort"));
        expected.put("client", List.of("tech.endorsed.signport.client.SignPortClient"));

        assertEquals(expected, parseEntrypoints(metadata),
                "fabric.mod.json entrypoints drift; update this test only when intentionally changing the loaded entrypoint set.");
    }

    @Test
    void mixinMetadataLoadsExactlyTheExpectedClientMixins() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/signport.mixins.json"));

        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
                "AbstractSignEditScreenMixin",
                "ScreenAccessor"));
        assertEquals(expected, parseStringArray(metadata, "client"),
                "Client mixin allow-list drift; update this test only when intentionally changing the loaded client mixin set.");
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
