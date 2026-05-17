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

    @Test
    void createRequestAcceptsNamesAtSharedMaxLength() {
        String max = "a".repeat(tech.endorsed.signport.world.AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
        AnchorSyncPayloads.CreateAnchorRequest request = new AnchorSyncPayloads.CreateAnchorRequest(max, "Spawn");

        assertTrue(request.name().length() <= tech.endorsed.signport.world.AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
        assertTrue(request.type().id().toString().contains("anchor_create_request"));
    }

    @Test
    void createResponseCarriesSuccessAndFailureText() {
        AnchorSyncPayloads.CreateAnchorResponse success = AnchorSyncPayloads.CreateAnchorResponse.accepted();
        AnchorSyncPayloads.CreateAnchorResponse failure = AnchorSyncPayloads.CreateAnchorResponse.failure("Duplicate name");

        assertTrue(success.success());
        assertTrue(success.errorMessage().isEmpty());
        assertFalse(failure.success());
        assertTrue(failure.errorMessage().contains("Duplicate"));
    }
}
