package tech.endorsed.signport.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorSyncPayloadsTest {
    @Test
    void initialSyncRequestWaitsForClientPlayerAndServerSupport() {
        assertFalse(AnchorSyncPayloads.shouldRequestInitialSync(false, true, false));
        assertFalse(AnchorSyncPayloads.shouldRequestInitialSync(true, false, false));
        assertFalse(AnchorSyncPayloads.shouldRequestInitialSync(true, true, true));
        assertTrue(AnchorSyncPayloads.shouldRequestInitialSync(true, true, false));
    }
}
