package tech.endorsed.signport.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void createRequestCodecRoundTripsNameAndGroupOnly() {
        AnchorSyncPayloads.CreateAnchorRequest request = new AnchorSyncPayloads.CreateAnchorRequest("spawn", "Spawn");
        RegistryFriendlyByteBuf buf = registryBuffer();

        AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_CODEC.encode(buf, request);
        AnchorSyncPayloads.CreateAnchorRequest decoded = AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_CODEC.decode(buf);

        assertEquals("spawn", decoded.name());
        assertEquals("Spawn", decoded.group());
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

    @Test
    void createResponseCodecRoundTripsStatusAndNullSafeText() {
        RegistryFriendlyByteBuf successBuf = registryBuffer();
        AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_CODEC.encode(successBuf, AnchorSyncPayloads.CreateAnchorResponse.accepted());
        AnchorSyncPayloads.CreateAnchorResponse success = AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_CODEC.decode(successBuf);

        RegistryFriendlyByteBuf failureBuf = registryBuffer();
        AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_CODEC.encode(failureBuf, AnchorSyncPayloads.CreateAnchorResponse.failure(null));
        AnchorSyncPayloads.CreateAnchorResponse failure = AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_CODEC.decode(failureBuf);

        assertTrue(success.success());
        assertEquals("", success.errorMessage());
        assertFalse(failure.success());
        assertEquals("", failure.errorMessage());
    }

    private static RegistryFriendlyByteBuf registryBuffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
