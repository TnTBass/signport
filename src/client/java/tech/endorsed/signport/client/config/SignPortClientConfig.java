package tech.endorsed.signport.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import tech.endorsed.signport.SignPort;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SignPortClientConfig {
    public static final String FILE_NAME = "signport-client.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Values values = Values.defaults();
    private static Path path;

    private SignPortClientConfig() {
    }

    public static Values get() {
        return values;
    }

    public static void load() {
        path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        values = load(path);
    }

    public static Values load(Path configPath) {
        Values defaults = Values.defaults();
        if (Files.notExists(configPath)) {
            save(configPath, defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                SignPort.LOGGER.warn("SignPort client config at {} is not a JSON object; using defaults.", configPath);
                return defaults;
            }
            JsonObject object = root.getAsJsonObject();
            return new Values(readBoolean(object, "hudHintEnabled", defaults.hudHintEnabled));
        } catch (Exception exception) {
            SignPort.LOGGER.warn("Could not read SignPort client config at {}; using defaults.", configPath, exception);
            return defaults;
        }
    }

    public static void save() {
        if (path == null) {
            path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        }
        save(path, values);
    }

    public static void setHudHintEnabled(boolean enabled) {
        values.hudHintEnabled = enabled;
    }

    private static void save(Path configPath, Values values) {
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(values, writer);
            }
        } catch (IOException exception) {
            SignPort.LOGGER.warn("Could not save SignPort client config at {}.", configPath, exception);
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null) return fallback;
        if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        SignPort.LOGGER.warn("SignPort client config option '{}' must be true or false; using default {}.", key, fallback);
        return fallback;
    }

    public static final class Values {
        public boolean hudHintEnabled;

        public Values(boolean hudHintEnabled) {
            this.hudHintEnabled = hudHintEnabled;
        }

        public static Values defaults() {
            return new Values(true);
        }
    }
}
