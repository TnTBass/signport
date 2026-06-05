package tech.endorsed.signport.status;

import org.junit.jupiter.api.Test;
import tech.endorsed.signport.internal.modstatus.ModStatusClientState;
import tech.endorsed.signport.internal.modstatus.StatusTone;
import tech.endorsed.signport.internal.modstatus.VersionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignPortStatusTest {
    @Test
    void configUsesSignPortStatusChannelAndPassiveMessages() {
        var config = SignPortStatus.config();

        assertEquals("signport", config.modId());
        assertEquals("SignPort", config.displayName());
        assertEquals("signport:status_version", config.payloadChannel());
        assertTrue(config.messages().helpFor(VersionStatus.DIFFERENT).contains("optional client features"));
        assertFalse(config.messages().helpFor(VersionStatus.SERVER_NOT_DETECTED).contains("incompatible"));
    }

    @Test
    void serverNotDetectedWaitsForPlayerAndGraceTicks() {
        assertFalse(SignPortStatus.shouldMarkServerNotDetected(false, false, SignPortStatus.SERVER_DETECTION_GRACE_TICKS + 1));
        assertFalse(SignPortStatus.shouldMarkServerNotDetected(true, true, SignPortStatus.SERVER_DETECTION_GRACE_TICKS + 1));
        assertFalse(SignPortStatus.shouldMarkServerNotDetected(true, false, SignPortStatus.SERVER_DETECTION_GRACE_TICKS - 1));
        assertTrue(SignPortStatus.shouldMarkServerNotDetected(true, false, SignPortStatus.SERVER_DETECTION_GRACE_TICKS));
    }

    @Test
    void clientStateCanBeDrivenWithoutMinecraftRuntime() {
        ModStatusClientState state = SignPortStatus.createClientState();

        state.unknown();
        assertEquals(VersionStatus.UNKNOWN, state.snapshot().status());

        state.markServerNotDetectedIfUnknown();
        assertEquals(VersionStatus.SERVER_NOT_DETECTED, state.snapshot().status());

        state.disconnected();
        assertEquals(VersionStatus.DISCONNECTED, state.snapshot().status());
    }

    @Test
    void sharedClientStateTracksJoinReceiveAndDisconnect() {
        SignPortStatus.onClientDisconnect();

        SignPortStatus.onClientJoin();
        assertEquals(VersionStatus.UNKNOWN, SignPortStatus.clientState().snapshot().status());

        SignPortStatus.onServerStatus(SignPortStatus.decodeServerStatus(SignPortStatus.encodeServerStatus()));
        assertEquals(VersionStatus.MATCHED, SignPortStatus.clientState().snapshot().status());

        SignPortStatus.onClientDisconnect();
        assertEquals(VersionStatus.DISCONNECTED, SignPortStatus.clientState().snapshot().status());
    }

    @Test
    void displayFormattingIncludesBuildOnlyWhenPresent() {
        assertEquals("2.2.2+mc26.1.2", SignPortStatus.versionWithBuild("2.2.2+mc26.1.2", null));
        assertEquals("2.2.2+mc26.1.2 (abc123)", SignPortStatus.versionWithBuild("2.2.2+mc26.1.2", "abc123"));
        assertEquals("Unknown", SignPortStatus.versionWithBuild("Unknown", ""));
    }

    @Test
    void toneColorsAreDefinedForConfigUi() {
        for (StatusTone tone : StatusTone.values()) {
            assertNotNull(SignPortStatus.toneColor(tone));
        }
    }
}
