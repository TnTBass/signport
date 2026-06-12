package tech.endorsed.signport.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.internal.modstatus.ModStatusServerStatus;
import tech.endorsed.signport.status.SignPortStatus;

public final class StatusPayloads {
    private static final int MAX_STATUS_PAYLOAD_BYTES = 512;
    public static final Identifier VERSION_ID =
            Identifier.fromNamespaceAndPath(SignPort.MOD_ID, SignPortStatus.SERVER_VERSION_CHANNEL_PATH);
    public static final Identifier REQUEST_ID = id("server_version_request");

    public static final CustomPacketPayload.Type<ServerVersionPayload> VERSION_TYPE =
            new CustomPacketPayload.Type<>(VERSION_ID);
    public static final CustomPacketPayload.Type<ServerVersionRequest> REQUEST_TYPE =
            new CustomPacketPayload.Type<>(REQUEST_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerVersionPayload> VERSION_CODEC = StreamCodec.of(
            StatusPayloads::writeServerVersion,
            StatusPayloads::readServerVersion);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerVersionRequest> REQUEST_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> ServerVersionRequest.INSTANCE);

    private StatusPayloads() {
    }

    public static boolean isVersionChannel(String channel) {
        return VERSION_TYPE.id().toString().equals(channel);
    }

    public record ServerVersionPayload(byte[] value) implements CustomPacketPayload {
        public ServerVersionPayload {
            value = value == null ? new byte[0] : value.clone();
        }

        @Override
        public byte[] value() {
            return value.clone();
        }

        public ModStatusServerStatus serverStatus() {
            return SignPortStatus.decodeServerStatus(value);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return VERSION_TYPE;
        }
    }

    public record ServerVersionRequest() implements CustomPacketPayload {
        public static final ServerVersionRequest INSTANCE = new ServerVersionRequest();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return REQUEST_TYPE;
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SignPort.MOD_ID, path);
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
