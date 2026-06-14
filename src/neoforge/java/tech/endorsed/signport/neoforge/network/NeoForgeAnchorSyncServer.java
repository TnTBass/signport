package tech.endorsed.signport.neoforge.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.network.AnchorSyncPayloads;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorCreation;
import tech.endorsed.signport.world.AnchorState;

import java.util.List;

public final class NeoForgeAnchorSyncServer {
    private NeoForgeAnchorSyncServer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeAnchorSyncServer::onPlayerRespawn);
    }

    public static void handleReady(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendFull(player);
            }
        });
    }

    public static void handleCreateAnchor(AnchorSyncPayloads.CreateAnchorRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handleCreateAnchor(payload, player);
            }
        });
    }

    public static void sendFull(ServerPlayer player) {
        if (!canSend(player, AnchorSyncPayloads.FULL_ID)) return;

        List<AnchorSyncPayloads.SyncedAnchor> anchors = AnchorState.peekServerState(player.level().getServer())
                .map(state -> state.anchors.stream()
                        .map(AnchorSyncPayloads.SyncedAnchor::from)
                        .toList())
                .orElse(List.of());
        PacketDistributor.sendToPlayer(player, new AnchorSyncPayloads.Full(anchors, permissions(player)));
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

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendFull(player);
        }
    }

    private static void sendDeltaToAll(MinecraftServer server, AnchorSyncPayloads.Delta delta) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (canSend(player, AnchorSyncPayloads.DELTA_ID)) {
                PacketDistributor.sendToPlayer(player, delta);
            }
        }
    }

    private static void handleCreateAnchor(AnchorSyncPayloads.CreateAnchorRequest payload, ServerPlayer player) {
        if (!canSend(player, AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_ID)) return;

        var source = player.createCommandSourceStack();
        if (!SignPortPermissions.canCreateAnchor(source)) {
            PacketDistributor.sendToPlayer(player, AnchorSyncPayloads.CreateAnchorResponse.failure("You do not have permission to create anchors"));
            return;
        }

        AnchorState state = AnchorState.getServerState(player.level().getServer());
        BlockPos pos = player.blockPosition();
        AnchorCreation.Result result = AnchorCreation.create(state, payload.name(), pos, player.level().dimension(), payload.group());
        if (!result.success()) {
            PacketDistributor.sendToPlayer(player, AnchorSyncPayloads.CreateAnchorResponse.failure(result.errorMessage()));
            return;
        }

        Anchor anchor = result.anchor();
        BlueMapIntegration.anchorCreated(player.level().getServer(), anchor);
        anchorCreated(player.level().getServer(), anchor);
        PacketDistributor.sendToPlayer(player, AnchorSyncPayloads.CreateAnchorResponse.accepted());
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

    private static boolean canSend(ServerPlayer player, net.minecraft.resources.Identifier channel) {
        return ((ICommonPacketListener) player.connection).hasChannel(channel);
    }
}
