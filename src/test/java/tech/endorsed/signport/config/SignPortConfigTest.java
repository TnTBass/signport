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
                  "safeTeleportSearch": false,
                  "anchorListPageSize": 25,
                  "defaultNearRadius": 64,
                  "bluemapEnabled": false
                }
                """);

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertFalse(config.teleportCommandDefault());
        assertFalse(config.signUseDefault());
        assertEquals(3, config.protectedActionOpLevel());
        assertFalse(config.crossDimensionPortalSigns());
        assertFalse(config.safeTeleportSearch());
        assertEquals(25, config.anchorListPageSize());
        assertEquals(64, config.defaultNearRadius());
        assertFalse(config.bluemapEnabled());
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
                  "safeTeleportSearch": false,
                  "anchorListPageSize": 0,
                  "defaultNearRadius": -1,
                  "bluemapEnabled": "yes"
                }
                """);

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertFalse(config.teleportCommandDefault());
        assertTrue(config.signUseDefault());
        assertEquals(2, config.protectedActionOpLevel());
        assertFalse(config.crossDimensionPortalSigns());
        assertFalse(config.safeTeleportSearch());
        assertEquals(10, config.anchorListPageSize());
        assertEquals(128, config.defaultNearRadius());
        assertTrue(config.bluemapEnabled());
    }

    @Test
    void defaultAnchorListPageSizeIsTen() {
        assertEquals(10, SignPortConfig.Values.defaults().anchorListPageSize());
    }

    @Test
    void defaultNearRadiusIsOneHundredTwentyEight() {
        assertEquals(128, SignPortConfig.Values.defaults().defaultNearRadius());
    }

    @Test
    void bluemapIntegrationDefaultsEnabled() {
        assertTrue(SignPortConfig.Values.defaults().bluemapEnabled());
    }

    @Test
    void malformedConfigUsesDefaults() throws IOException {
        Path path = tempDir.resolve(SignPortConfig.FILE_NAME);
        Files.writeString(path, "{");

        SignPortConfig.Values config = SignPortConfig.load(path, LOGGER);

        assertEquals(SignPortConfig.Values.defaults(), config);
    }
}
