package tech.endorsed.signport.neoforge.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tech.endorsed.signport.network.SignPortStatusNetworking;
import tech.endorsed.signport.network.StatusPayloads;

public final class NeoForgeSignPortStatusNetworking {
    private NeoForgeSignPortStatusNetworking() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeSignPortStatusNetworking::onPlayerLoggedIn);
    }

    public static void handleServerVersionRequest(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendServerVersionIfSupported(player);
            }
        });
    }

    public static boolean sendServerVersionIfSupported(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return SignPortStatusNetworking.sendConfiguredServerVersionIfSupported(
                channel -> StatusPayloads.isVersionChannel(channel)
                        && ((ICommonPacketListener) player.connection).hasChannel(StatusPayloads.VERSION_ID),
                (channel, payload) -> {
                    if (StatusPayloads.isVersionChannel(channel)) {
                        PacketDistributor.sendToPlayer(player, new StatusPayloads.ServerVersionPayload(payload));
                    }
                }
        );
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SignPortStatusNetworking.shouldSendServerVersionOnJoin(true)) {
            sendServerVersionIfSupported(player);
        }
    }
}
