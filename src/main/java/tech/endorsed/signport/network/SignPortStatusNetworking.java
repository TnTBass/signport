package tech.endorsed.signport.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import tech.endorsed.signport.internal.modstatus.ModStatusVersionPayload;
import tech.endorsed.signport.internal.modstatus.VersionMismatchSeverity;
import tech.endorsed.signport.status.SignPortStatus;

public final class SignPortStatusNetworking {
    public static final int SERVER_VERSION_REQUEST_INTERVAL_TICKS = 40;
    public static final int NO_SERVER_VERSION_REQUEST_TICK = -1;

    private SignPortStatusNetworking() {
    }

    public static void register() {
        StatusPayloads.registerClientbound();
        StatusPayloads.registerServerbound();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (shouldSendServerVersionOnJoin(player != null)) {
                sendServerVersionIfSupported(player);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(StatusPayloads.REQUEST_TYPE, (payload, context) ->
                context.server().execute(() -> sendServerVersionIfSupported(context.player())));
    }

    public static boolean sendServerVersionIfSupported(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return sendConfiguredServerVersionIfSupported(
                channel -> StatusPayloads.isVersionChannel(channel)
                        && ServerPlayNetworking.canSend(player, StatusPayloads.VERSION_TYPE),
                (channel, payload) -> {
                    if (StatusPayloads.isVersionChannel(channel)) {
                        ServerPlayNetworking.send(player, new StatusPayloads.ServerVersionPayload(payload));
                    }
                }
        );
    }

    static boolean sendConfiguredServerVersionIfSupported(
            ModStatusVersionPayload.PayloadSupport support,
            ModStatusVersionPayload.PayloadSender sender
    ) {
        return ModStatusVersionPayload.sendServerStatusIfSupported(
                SignPortStatus.config(),
                VersionMismatchSeverity.WARN,
                support,
                sender
        );
    }

    public static boolean shouldRequestServerVersion(
            boolean playerPresent,
            boolean canSendRequest,
            boolean statusPayloadReceived,
            int currentTick,
            int lastRequestTick
    ) {
        return playerPresent
                && canSendRequest
                && !statusPayloadReceived
                && (lastRequestTick == NO_SERVER_VERSION_REQUEST_TICK
                || currentTick - lastRequestTick >= SERVER_VERSION_REQUEST_INTERVAL_TICKS);
    }

    public static boolean shouldSendServerVersionOnJoin(boolean playerPresent) {
        return playerPresent;
    }
}
