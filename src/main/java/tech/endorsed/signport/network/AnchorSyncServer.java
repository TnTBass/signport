package tech.endorsed.signport.network;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorCreation;
import tech.endorsed.signport.world.AnchorState;

import java.util.List;

public final class AnchorSyncServer {
    private AnchorSyncServer() {
    }

    public static void register() {
        AnchorSyncPayloads.registerClientbound();
        AnchorSyncPayloads.registerServerbound();

        ServerPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.READY_TYPE, (payload, context) ->
                context.server().execute(() -> sendFull(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_TYPE, (payload, context) ->
                context.server().execute(() -> handleCreateAnchor(payload, context.player())));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                sendFull(newPlayer));
    }

    public static void sendFull(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, AnchorSyncPayloads.FULL_TYPE)) return;

        List<AnchorSyncPayloads.SyncedAnchor> anchors = AnchorState.peekServerState(player.level().getServer())
                .map(state -> state.anchors.stream()
                        .map(AnchorSyncPayloads.SyncedAnchor::from)
                        .toList())
                .orElse(List.of());
        ServerPlayNetworking.send(player, new AnchorSyncPayloads.Full(anchors, permissions(player)));
    }

    public static void sendFullToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendFull(player);
        }
    }

    public static void anchorCreated(MinecraftServer server, Anchor anchor) {
        sendDeltaToAll(server, AnchorSyncPayloads.Delta.create(AnchorSyncPayloads.SyncedAnchor.from(anchor)));
    }

    public static void anchorUpdated(MinecraftServer server, Anchor anchor) {
        sendDeltaToAll(server, AnchorSyncPayloads.Delta.update(AnchorSyncPayloads.SyncedAnchor.from(anchor)));
    }

    public static void anchorDeleted(MinecraftServer server, String name, ResourceKey<Level> dimension) {
        sendDeltaToAll(server, AnchorSyncPayloads.Delta.delete(name, dimension));
    }

    private static void sendDeltaToAll(MinecraftServer server, AnchorSyncPayloads.Delta delta) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, AnchorSyncPayloads.DELTA_TYPE)) {
                ServerPlayNetworking.send(player, delta);
            }
        }
    }

    private static void handleCreateAnchor(AnchorSyncPayloads.CreateAnchorRequest payload, ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE)) return;

        var source = player.createCommandSourceStack();
        if (!SignPortPermissions.canCreateAnchor(source)) {
            ServerPlayNetworking.send(player, AnchorSyncPayloads.CreateAnchorResponse.failure("You do not have permission to create anchors"));
            return;
        }

        AnchorState state = AnchorState.getServerState(player.level().getServer());
        BlockPos pos = player.blockPosition();
        AnchorCreation.Result result = AnchorCreation.create(state, payload.name(), pos, player.level().dimension(), payload.group());
        if (!result.success()) {
            ServerPlayNetworking.send(player, AnchorSyncPayloads.CreateAnchorResponse.failure(result.errorMessage()));
            return;
        }

        Anchor anchor = result.anchor();
        BlueMapIntegration.anchorCreated(player.level().getServer(), anchor);
        anchorCreated(player.level().getServer(), anchor);
        ServerPlayNetworking.send(player, AnchorSyncPayloads.CreateAnchorResponse.accepted());
    }

    private static AnchorSyncPayloads.PermissionSnapshot permissions(ServerPlayer player) {
        var source = player.createCommandSourceStack();
        return new AnchorSyncPayloads.PermissionSnapshot(
                SignPortPermissions.canCreateSign(player),
                SignPortPermissions.canEditSign(player),
                SignPortPermissions.canCreateAnchor(source),
                SignPortPermissions.canDeleteAnchor(source),
                SignPortPermissions.canListAnchors(source),
                SignPortPermissions.canUseTeleportCommand(source));
    }
}
