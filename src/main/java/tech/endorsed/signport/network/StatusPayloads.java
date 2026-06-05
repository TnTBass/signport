package tech.endorsed.signport.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.status.SignPortStatus;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StatusPayloads {
    private static final int MAX_STATUS_PAYLOAD_BYTES = 512;

    public static final CustomPacketPayload.Type<ServerVersionPayload> VERSION_TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SignPort.MOD_ID, SignPortStatus.SERVER_VERSION_CHANNEL_PATH));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerVersionPayload> VERSION_CODEC = StreamCodec.of(
            StatusPayloads::writeServerVersion,
            StatusPayloads::readServerVersion);

    private static final AtomicBoolean clientboundRegistered = new AtomicBoolean();

    private StatusPayloads() {
    }

    public static void registerClientbound() {
        if (!clientboundRegistered.compareAndSet(false, true)) return;
        PayloadTypeRegistry.clientboundPlay().register(VERSION_TYPE, VERSION_CODEC);
    }

    public record ServerVersionPayload(byte[] value) implements CustomPacketPayload {
        public ServerVersionPayload {
            value = value == null ? new byte[0] : value.clone();
        }

        @Override
        public byte[] value() {
            return value.clone();
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return VERSION_TYPE;
        }
    }

    private static void writeServerVersion(RegistryFriendlyByteBuf buf, ServerVersionPayload payload) {
        byte[] value = payload.value() == null ? new byte[0] : payload.value();
        if (value.length > MAX_STATUS_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("status payload too large");
        }
        buf.writeVarInt(value.length);
        buf.writeBytes(value);
    }

    private static ServerVersionPayload readServerVersion(RegistryFriendlyByteBuf buf) {
        int length = buf.readVarInt();
        if (length < 0 || length > MAX_STATUS_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("status payload length out of bounds");
        }
        byte[] value = new byte[length];
        buf.readBytes(value);
        return new ServerVersionPayload(value);
    }
}
