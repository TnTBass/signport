package tech.endorsed.signport.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.status.SignPortStatus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPayloadsTest {
    @Test
    void statusPayloadUsesDedicatedSignPortChannel() {
        assertEquals("signport:status_version", StatusPayloads.VERSION_TYPE.id().toString());
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
}
