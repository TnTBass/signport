package tech.endorsed.signport.internal.modstatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModStatusKitTest {
    @Test
    void clientStateStartsDisconnectedAndDisplaysPassiveText() {
        ModStatusClientState state = ModStatusClientState.create(config("2.2.2+mc26.1.2", null));

        ModStatusDisplay display = state.display();

        assertEquals(VersionStatus.DISCONNECTED, state.snapshot().status());
        assertEquals("Disconnected", display.statusLabel());
        assertEquals(StatusTone.GRAY, display.tone());
        assertTrue(display.helpText().contains("Not connected"));
    }

    @Test
    void connectedMatchingVersionDisplaysMatch() {
        ModStatusClientState state = ModStatusClientState.create(config("2.2.2+mc26.1.2", null));

        state.connected("2.2.2+mc26.1.2");

        ModStatusDisplay display = state.display();
        assertEquals(VersionStatus.MATCHED, state.snapshot().status());
        assertEquals("Matched", display.statusLabel());
        assertEquals("2.2.2", display.serverVersion());
        assertEquals("mc26.1.2", display.serverBuild());
        assertEquals(StatusTone.GREEN, display.tone());
    }

    @Test
    void differentBuildWithMatchingVersionUsesDiagnosticTone() {
        ModStatusClientState state = ModStatusClientState.create(config("2.2.2+mc26.1.2", "client-build"));

        state.connected(ModStatusServerStatus.of("2.2.2+mc26.1.2", "server-build", VersionMismatchSeverity.WARN));

        ModStatusDisplay display = state.display();
        assertEquals(VersionStatus.MATCHED, state.snapshot().status());
        assertEquals(StatusTone.TEAL, display.tone());
        assertEquals("client-build", display.clientBuild());
        assertEquals("server-build", display.serverBuild());
    }

    @Test
    void structuredServerStatusPayloadRoundTripsVersionBuildAndSeverity() {
        byte[] payload = ModStatusVersionPayload.encodeServerStatus(
                "2.2.2+mc26.1.2",
                "abc123",
                VersionMismatchSeverity.BREAKING);

        ModStatusServerStatus decoded = ModStatusVersionPayload.decodeServerStatus(payload);

        assertEquals("2.2.2", decoded.serverVersion());
        assertEquals("abc123", decoded.serverBuild());
        assertEquals(VersionMismatchSeverity.BREAKING, decoded.versionMismatchSeverity());
        assertFalse(new String(payload, java.nio.charset.StandardCharsets.UTF_8).contains("\r"));
    }

    @Test
    void legacyServerVersionPayloadDecodesWithWarnSeverity() {
        byte[] payload = "2.2.2+mc26.1.2".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        ModStatusServerStatus decoded = ModStatusVersionPayload.decodeServerStatus(payload);

        assertEquals("2.2.2", decoded.serverVersion());
        assertEquals("mc26.1.2", decoded.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, decoded.versionMismatchSeverity());
    }

    private static ModStatusConfig config(String version, String build) {
        return ModStatusConfig.builder()
                .modId("signport")
                .displayName("SignPort")
                .clientVersion(version)
                .clientBuild(build)
                .payloadChannel("signport", "status_version")
                .build();
    }
}
