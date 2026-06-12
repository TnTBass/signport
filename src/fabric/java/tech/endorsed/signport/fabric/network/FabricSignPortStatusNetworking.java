package tech.endorsed.signport.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import tech.endorsed.signport.network.SignPortStatusNetworking;
import tech.endorsed.signport.network.StatusPayloads;

public final class FabricSignPortStatusNetworking {
    private FabricSignPortStatusNetworking() {
    }

    public static void register() {
        FabricPayloads.registerClientbound();
        FabricPayloads.registerServerbound();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (SignPortStatusNetworking.shouldSendServerVersionOnJoin(player != null)) {
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
        return SignPortStatusNetworking.sendConfiguredServerVersionIfSupported(
                channel -> StatusPayloads.isVersionChannel(channel)
                        && ServerPlayNetworking.canSend(player, StatusPayloads.VERSION_TYPE),
                (channel, payload) -> {
                    if (StatusPayloads.isVersionChannel(channel)) {
                        ServerPlayNetworking.send(player, new StatusPayloads.ServerVersionPayload(payload));
                    }
                }
        );
    }
}
