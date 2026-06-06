package tech.endorsed.signport.status;

import org.junit.jupiter.api.Test;
import tech.endorsed.signport.BuildInfo;
import tech.endorsed.signport.internal.modstatus.ModStatusClientState;
import tech.endorsed.signport.internal.modstatus.ModStatusDisplay;
import tech.endorsed.signport.internal.modstatus.StatusTone;
import tech.endorsed.signport.internal.modstatus.VersionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignPortStatusTest {
    @Test
    void configUsesSignPortStatusChannelAndPassiveMessages() {
        var config = SignPortStatus.config();

        assertEquals("signport", config.modId());
        assertEquals("SignPort", config.displayName());
        assertEquals("signport:server_version", config.payloadChannel());
        assertEquals(BuildInfo.BUILD_NUMBER, config.clientBuild());
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
        assertEquals("2.2.2+abc123", SignPortStatus.versionWithBuild("2.2.2", "abc123"));
        assertEquals("Unknown", SignPortStatus.versionWithBuild("Unknown", ""));
    }

    @Test
    void toneColorsAreDefinedForConfigUi() {
        assertEquals(0x55FF55, SignPortStatus.toneColor(StatusTone.GREEN));
        assertEquals(0x55FFFF, SignPortStatus.toneColor(StatusTone.TEAL));
        assertEquals(0xFFAA00, SignPortStatus.toneColor(StatusTone.ORANGE));
        assertEquals(0xFF5555, SignPortStatus.toneColor(StatusTone.RED));
        assertEquals(0xAAAAAA, SignPortStatus.toneColor(StatusTone.GRAY));
    }

    @Test
    void displayHelperMatchesReferenceSquareIndicatorPattern() {
        ModStatusDisplay display = SignPortStatus.clientState().display();

        assertEquals("\u25A0", SignPortStatusDisplay.STATUS_SQUARE);
        assertEquals(8, SignPortStatusDisplay.STATUS_SQUARE_SIZE);
        assertEquals(0xFF222222, SignPortStatusDisplay.STATUS_SQUARE_BORDER_COLOR);
        assertEquals(0xFF55FF55, SignPortStatusDisplay.toneColor(StatusTone.GREEN));
        assertEquals(0xFF55FFFF, SignPortStatusDisplay.toneColor(StatusTone.TEAL));
        assertEquals(0xFFFFAA00, SignPortStatusDisplay.toneColor(StatusTone.ORANGE));
        assertEquals(0xFFFF5555, SignPortStatusDisplay.toneColor(StatusTone.RED));
        assertEquals(0xFFAAAAAA, SignPortStatusDisplay.toneColor(StatusTone.GRAY));
        assertTrue(SignPortStatusDisplay.tooltipText(display).get(0).contains("SignPort"));
        assertEquals("2.2.2", SignPortStatusDisplay.versionWithBuild("2.2.2", "dev"));
        assertEquals("2.2.2+abc123", SignPortStatusDisplay.versionWithBuild("2.2.2", "abc123"));
    }
}
