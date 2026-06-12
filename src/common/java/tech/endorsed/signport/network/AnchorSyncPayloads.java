package tech.endorsed.signport.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorCreation;

import java.util.ArrayList;
import java.util.List;

public final class AnchorSyncPayloads {
    public static final CustomPacketPayload.Type<Full> FULL_TYPE =
            new CustomPacketPayload.Type<>(id("anchor_sync_full"));
    public static final CustomPacketPayload.Type<Delta> DELTA_TYPE =
            new CustomPacketPayload.Type<>(id("anchor_sync_delta"));
    public static final CustomPacketPayload.Type<Ready> READY_TYPE =
            new CustomPacketPayload.Type<>(id("anchor_sync_ready"));
    public static final CustomPacketPayload.Type<CreateAnchorRequest> CREATE_ANCHOR_REQUEST_TYPE =
            new CustomPacketPayload.Type<>(id("anchor_create_request"));
    public static final CustomPacketPayload.Type<CreateAnchorResponse> CREATE_ANCHOR_RESPONSE_TYPE =
            new CustomPacketPayload.Type<>(id("anchor_create_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Full> FULL_CODEC = StreamCodec.of(
            AnchorSyncPayloads::writeFull,
            AnchorSyncPayloads::readFull);
    public static final StreamCodec<RegistryFriendlyByteBuf, Delta> DELTA_CODEC = StreamCodec.of(
            AnchorSyncPayloads::writeDelta,
            AnchorSyncPayloads::readDelta);
    public static final StreamCodec<RegistryFriendlyByteBuf, Ready> READY_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> new Ready());
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateAnchorRequest> CREATE_ANCHOR_REQUEST_CODEC = StreamCodec.of(
            AnchorSyncPayloads::writeCreateAnchorRequest,
            AnchorSyncPayloads::readCreateAnchorRequest);
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateAnchorResponse> CREATE_ANCHOR_RESPONSE_CODEC = StreamCodec.of(
            AnchorSyncPayloads::writeCreateAnchorResponse,
            AnchorSyncPayloads::readCreateAnchorResponse);
    private AnchorSyncPayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SignPort.MOD_ID, path);
    }

    public static boolean shouldRequestInitialSync(boolean playerPresent, boolean canSendReady, boolean alreadyRequested) {
        return playerPresent && canSendReady && !alreadyRequested;
    }

    public record Full(List<SyncedAnchor> anchors, PermissionSnapshot permissions) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return FULL_TYPE;
        }
    }

    public record Delta(Action action, SyncedAnchor anchor, String name, ResourceKey<Level> dimension)
            implements CustomPacketPayload {
        public static Delta create(SyncedAnchor anchor) {
            return new Delta(Action.CREATE, anchor, anchor.name(), anchor.dimension());
        }

        public static Delta update(SyncedAnchor anchor) {
            return new Delta(Action.UPDATE, anchor, anchor.name(), anchor.dimension());
        }

        public static Delta delete(String name, ResourceKey<Level> dimension) {
            return new Delta(Action.DELETE, null, name, dimension);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return DELTA_TYPE;
        }
    }

    public enum Action {
        CREATE,
        DELETE,
        UPDATE
    }

    public record Ready() implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return READY_TYPE;
        }
    }

    public record CreateAnchorRequest(String name, String group) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return CREATE_ANCHOR_REQUEST_TYPE;
        }
    }

    public record CreateAnchorResponse(boolean success, String errorMessage) implements CustomPacketPayload {
        public static CreateAnchorResponse accepted() {
            return new CreateAnchorResponse(true, "");
        }

        public static CreateAnchorResponse failure(String errorMessage) {
            return new CreateAnchorResponse(false, errorMessage == null ? "" : errorMessage);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return CREATE_ANCHOR_RESPONSE_TYPE;
        }
    }

    public record SyncedAnchor(
            String name,
            BlockPos pos,
            ResourceKey<Level> dimension,
            String group,
            long createdAt
    ) {
        public static SyncedAnchor from(Anchor anchor) {
            return new SyncedAnchor(anchor.name, anchor.pos, anchor.dimension, anchor.group, anchor.createdAt);
        }
    }

    public record PermissionSnapshot(
            boolean canCreatePortSign,
            boolean canEditPortSign,
            boolean canCreateAnchor,
            boolean canDeleteAnchor,
            boolean canListAnchors,
            boolean canUseTeleportCommand
    ) {
        public static PermissionSnapshot empty() {
            return new PermissionSnapshot(false, false, false, false, false, false);
        }
    }

    private static void writeFull(RegistryFriendlyByteBuf buf, Full payload) {
        buf.writeVarInt(payload.anchors().size());
        for (SyncedAnchor anchor : payload.anchors()) {
            writeAnchor(buf, anchor);
        }
        writePermissions(buf, payload.permissions());
    }

    private static Full readFull(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SyncedAnchor> anchors = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            anchors.add(readAnchor(buf));
        }
        return new Full(List.copyOf(anchors), readPermissions(buf));
    }

    private static void writeDelta(RegistryFriendlyByteBuf buf, Delta payload) {
        buf.writeEnum(payload.action());
        if (payload.action() == Action.DELETE) {
            buf.writeUtf(payload.name());
            buf.writeResourceKey(payload.dimension());
        } else {
            writeAnchor(buf, payload.anchor());
        }
    }

    private static Delta readDelta(RegistryFriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        if (action == Action.DELETE) {
            return Delta.delete(buf.readUtf(), buf.readResourceKey(Registries.DIMENSION));
        }
        SyncedAnchor anchor = readAnchor(buf);
        return new Delta(action, anchor, anchor.name(), anchor.dimension());
    }

    private static void writeCreateAnchorRequest(RegistryFriendlyByteBuf buf, CreateAnchorRequest payload) {
        buf.writeUtf(payload.name() == null ? "" : payload.name(), AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
        buf.writeUtf(payload.group() == null ? "" : payload.group());
    }

    private static CreateAnchorRequest readCreateAnchorRequest(RegistryFriendlyByteBuf buf) {
        return new CreateAnchorRequest(
                buf.readUtf(AnchorCreation.MAX_ANCHOR_NAME_LENGTH),
                buf.readUtf());
    }

    private static void writeCreateAnchorResponse(RegistryFriendlyByteBuf buf, CreateAnchorResponse payload) {
        buf.writeBoolean(payload.success());
        buf.writeUtf(payload.errorMessage() == null ? "" : payload.errorMessage());
    }

    private static CreateAnchorResponse readCreateAnchorResponse(RegistryFriendlyByteBuf buf) {
        return new CreateAnchorResponse(buf.readBoolean(), buf.readUtf());
    }

    private static void writeAnchor(RegistryFriendlyByteBuf buf, SyncedAnchor anchor) {
        buf.writeUtf(anchor.name());
        buf.writeBlockPos(anchor.pos());
        buf.writeResourceKey(anchor.dimension());
        buf.writeUtf(anchor.group() == null ? "" : anchor.group());
        buf.writeLong(anchor.createdAt());
    }

    private static SyncedAnchor readAnchor(RegistryFriendlyByteBuf buf) {
        return new SyncedAnchor(
                buf.readUtf(),
                buf.readBlockPos(),
                buf.readResourceKey(Registries.DIMENSION),
                buf.readUtf(),
                buf.readLong());
    }

    private static void writePermissions(RegistryFriendlyByteBuf buf, PermissionSnapshot permissions) {
        buf.writeBoolean(permissions.canCreatePortSign());
        buf.writeBoolean(permissions.canEditPortSign());
        buf.writeBoolean(permissions.canCreateAnchor());
        buf.writeBoolean(permissions.canDeleteAnchor());
        buf.writeBoolean(permissions.canListAnchors());
        buf.writeBoolean(permissions.canUseTeleportCommand());
    }

    private static PermissionSnapshot readPermissions(RegistryFriendlyByteBuf buf) {
        return new PermissionSnapshot(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean());
    }
}
