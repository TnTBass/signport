package tech.endorsed.signport.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.status.SignPortStatus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPayloadsTest {
    @Test
    void statusPayloadUsesDedicatedSignPortChannel() {
        assertEquals("signport:server_version", StatusPayloads.VERSION_ID.toString());
        assertEquals("signport:server_version_request", StatusPayloads.REQUEST_ID.toString());
    }

    @Test
    void statusPayloadCanValidateMskChannelName() {
        assertTrue(StatusPayloads.isVersionChannel("signport:server_version"));
        assertFalse(StatusPayloads.isVersionChannel("signport:status_version"));
        assertFalse(StatusPayloads.isVersionChannel(null));
    }

    @Test
    void statusPayloadCodecRoundTripsBytes() {
        byte[] value = SignPortStatus.encodeServerStatus();
        StatusPayloads.ServerVersionPayload payload = new StatusPayloads.ServerVersionPayload(value);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        try {
            StatusPayloads.VERSION_CODEC.encode(buf, payload);
            StatusPayloads.ServerVersionPayload decoded = StatusPayloads.VERSION_CODEC.decode(buf);

            assertArrayEquals(value, decoded.value());
            assertEquals(StatusPayloads.VERSION_TYPE, decoded.type());
            assertTrue(decoded.value().length > 0);
            assertEquals(SignPortStatus.decodeServerStatus(value).serverVersionInfo(), decoded.serverStatus().serverVersionInfo());
        } finally {
            buf.release();
        }
    }

    @Test
    void serverVersionRequestPayloadCodecRoundTripsEmptyPayload() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        try {
            StatusPayloads.REQUEST_CODEC.encode(buf, StatusPayloads.ServerVersionRequest.INSTANCE);
            StatusPayloads.ServerVersionRequest decoded = StatusPayloads.REQUEST_CODEC.decode(buf);

            assertEquals(0, buf.readableBytes());
            assertEquals(StatusPayloads.REQUEST_TYPE, decoded.type());
        } finally {
            buf.release();
        }
    }

    @Test
    void statusPayloadCodecRejectsTruncatedPayload() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        try {
            buf.writeVarInt(10);
            buf.writeByte(1);

            assertThrows(IndexOutOfBoundsException.class, () -> StatusPayloads.VERSION_CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void statusNetworkingSendsOnlyWhenServerVersionPayloadIsSupported() {
        byte[][] sent = new byte[1][];

        assertFalse(SignPortStatusNetworking.sendConfiguredServerVersionIfSupported(
                channel -> false,
                (channel, payload) -> sent[0] = payload));
        assertNull(sent[0]);

        assertTrue(SignPortStatusNetworking.sendConfiguredServerVersionIfSupported(
                channel -> true,
                (channel, payload) -> sent[0] = payload));
        assertEquals(SignPortStatus.config().payloadChannel(), StatusPayloads.VERSION_TYPE.id().toString());
        assertEquals(SignPortStatus.config().clientVersionInfo(), SignPortStatus.decodeServerStatus(sent[0]).serverVersionInfo());
    }

    @Test
    void clientRequestsServerVersionOnlyWhenRequestChannelIsReadyAndStatusMissing() {
        assertFalse(SignPortStatusNetworking.shouldRequestServerVersion(false, true, false, 0, -1));
        assertFalse(SignPortStatusNetworking.shouldRequestServerVersion(true, false, false, 0, -1));
        assertFalse(SignPortStatusNetworking.shouldRequestServerVersion(true, true, true, 0, -1));
        assertFalse(SignPortStatusNetworking.shouldRequestServerVersion(true, true, false, 20, 0));
        assertTrue(SignPortStatusNetworking.shouldRequestServerVersion(true, true, false, 0, -1));
        assertTrue(SignPortStatusNetworking.shouldRequestServerVersion(true, true, false, 40, 0));
    }

    @Test
    void clientKeepsRequestingServerVersionAfterNotDetectedTimeoutUntilPayloadArrives() {
        assertTrue(SignPortStatusNetworking.shouldRequestServerVersion(
                true,
                true,
                false,
                120,
                SignPortStatusNetworking.NO_SERVER_VERSION_REQUEST_TICK));
        assertFalse(SignPortStatusNetworking.shouldRequestServerVersion(true, true, true, 120, 80));
    }

    @Test
    void resetStatusRequestTickAllowsImmediateRequestAfterReconnect() {
        assertTrue(SignPortStatusNetworking.shouldRequestServerVersion(
                true,
                true,
                false,
                0,
                SignPortStatusNetworking.NO_SERVER_VERSION_REQUEST_TICK));
    }

    @Test
    void serverJoinPushAttemptsStatusWhenPlayerIsPresent() {
        assertFalse(SignPortStatusNetworking.shouldSendServerVersionOnJoin(false));
        assertTrue(SignPortStatusNetworking.shouldSendServerVersionOnJoin(true));
    }
}
