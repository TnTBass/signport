package tech.endorsed.signport;

import net.fabricmc.api.EnvType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

}
