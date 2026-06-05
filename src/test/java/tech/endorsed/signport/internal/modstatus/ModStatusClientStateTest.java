package tech.endorsed.signport.internal.modstatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModStatusClientStateTest {
    @Test
    void markServerNotDetectedOnlyChangesUnknownSnapshots() {
        ModStatusClientState state = ModStatusClientState.create(config());

        assertFalse(state.markServerNotDetectedIfUnknown());
        assertEquals(VersionStatus.DISCONNECTED, state.snapshot().status());

        state.unknown();
        assertTrue(state.markServerNotDetectedIfUnknown());
        assertEquals(VersionStatus.SERVER_NOT_DETECTED, state.snapshot().status());
    }

    private static ModStatusConfig config() {
        return ModStatusConfig.builder()
                .modId("signport")
                .displayName("SignPort")
                .clientVersion("2.2.2+mc26.1.2")
                .payloadChannel("signport", "status_version")
                .build();
    }
}
