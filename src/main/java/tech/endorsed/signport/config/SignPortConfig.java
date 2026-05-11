package tech.endorsed.signport.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import tech.endorsed.signport.SignPort;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SignPortConfig {
    public static final String FILE_NAME = "signport.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Values values = Values.defaults();

    private SignPortConfig() {
    }

    public static Values get() {
        return values;
    }

    public static void load() {
        values = load(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME), SignPort.LOGGER);
    }

    public static Values load(Path path, Logger logger) {
        Values defaults = Values.defaults();

        if (Files.notExists(path)) {
            writeDefaults(path, defaults, logger);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                logger.warn("SignPort config at {} is not a JSON object; using defaults.", path);
                return defaults;
            }

            JsonObject object = root.getAsJsonObject();
            return new Values(
                    readBoolean(object, "teleportCommandDefault", defaults.teleportCommandDefault(), logger),
                    readBoolean(object, "signUseDefault", defaults.signUseDefault(), logger),
                    readOpLevel(object, defaults.protectedActionOpLevel(), logger),
                    readBoolean(object, "crossDimensionPortalSigns", defaults.crossDimensionPortalSigns(), logger),
                    readBoolean(object, "safeTeleportSearch", defaults.safeTeleportSearch(), logger),
                    readBoundedInt(object, "anchorListPageSize", defaults.anchorListPageSize(), 1, 100, logger),
                    readBoundedInt(object, "defaultNearRadius", defaults.defaultNearRadius(), 1, 10000, logger),
                    readBoolean(object, "bluemapEnabled", defaults.bluemapEnabled(), logger)
            );
        } catch (Exception exception) {
            logger.warn("Could not read SignPort config at {}; using defaults.", path, exception);
            return defaults;
        }
    }

    private static void writeDefaults(Path path, Values defaults, Logger logger) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(defaults, writer);
            }
        } catch (IOException exception) {
            logger.warn("Could not create SignPort config at {}; using defaults.", path, exception);
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback, Logger logger) {
        JsonElement element = object.get(key);
        if (element == null) return fallback;

        if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }

        logger.warn("SignPort config option '{}' must be true or false; using default {}.", key, fallback);
        return fallback;
    }

    private static int readOpLevel(JsonObject object, int fallback, Logger logger) {
        JsonElement element = object.get("protectedActionOpLevel");
        if (element == null) return fallback;

        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            int value = primitive.getAsInt();
            if (value >= 0 && value <= 4) {
                return value;
            }
        }

        logger.warn("SignPort config option 'protectedActionOpLevel' must be an integer from 0 to 4; using default {}.", fallback);
        return fallback;
    }

    private static int readBoundedInt(JsonObject object, String key, int fallback, int min, int max, Logger logger) {
        JsonElement element = object.get(key);
        if (element == null) return fallback;

        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            int value = primitive.getAsInt();
            if (value >= min && value <= max) {
                return value;
            }
        }

        logger.warn("SignPort config option '{}' must be an integer from {} to {}; using default {}.",
                key, min, max, fallback);
        return fallback;
    }

    public record Values(
            boolean teleportCommandDefault,
            boolean signUseDefault,
            int protectedActionOpLevel,
            boolean crossDimensionPortalSigns,
            boolean safeTeleportSearch,
            int anchorListPageSize,
            int defaultNearRadius,
            boolean bluemapEnabled
    ) {
        public static Values defaults() {
            return new Values(true, true, 2, true, true, 10, 128, true);
        }
    }
}
