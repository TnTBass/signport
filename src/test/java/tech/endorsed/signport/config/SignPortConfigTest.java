package tech.endorsed.signport.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignPortConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignPortConfigTest.class);

    @TempDir
    Path tempDir;

    @Test
    void missingConfigCreatesDefaults() {
        Path path = tempDir.resolve(SignPortConfig.FILE_NAME);

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertEquals(SignPortConfig.Values.defaults(), config);
        assertTrue(Files.exists(path));
    }

    @Test
    void readsConfiguredValues() throws IOException {
        Path path = tempDir.resolve(SignPortConfig.FILE_NAME);
        Files.writeString(path, """
                {
                  "teleportCommandDefault": false,
                  "signUseDefault": false,
                  "protectedActionOpLevel": 3,
                  "crossDimensionPortalSigns": false,
                  "safeTeleportSearch": false
                }
                """);

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertFalse(config.teleportCommandDefault());
        assertFalse(config.signUseDefault());
        assertEquals(3, config.protectedActionOpLevel());
        assertFalse(config.crossDimensionPortalSigns());
        assertFalse(config.safeTeleportSearch());
    }

    @Test
    void invalidOptionValuesFallBackIndividually() throws IOException {
        Path path = tempDir.resolve(SignPortConfig.FILE_NAME);
        Files.writeString(path, """
                {
                  "teleportCommandDefault": false,
                  "signUseDefault": "yes",
                  "protectedActionOpLevel": 12,
                  "crossDimensionPortalSigns": false,
                  "safeTeleportSearch": false
                }
                """);

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertFalse(config.teleportCommandDefault());
        assertTrue(config.signUseDefault());
        assertEquals(2, config.protectedActionOpLevel());
        assertFalse(config.crossDimensionPortalSigns());
        assertFalse(config.safeTeleportSearch());
    }

    @Test
    void malformedConfigUsesDefaults() throws IOException {
        Path path = tempDir.resolve(SignPortConfig.FILE_NAME);
        Files.writeString(path, "{");

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertEquals(SignPortConfig.Values.defaults(), config);
    }
}
