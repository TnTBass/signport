package tech.endorsed.signport.neoforge.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.network.AnchorSyncPayloads;
import tech.endorsed.signport.network.StatusPayloads;

public final class NeoForgePayloads {
    private NeoForgePayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SignPort.MOD_ID).optional();

        registrar.playToClient(AnchorSyncPayloads.FULL_TYPE, AnchorSyncPayloads.FULL_CODEC);
        registrar.playToClient(AnchorSyncPayloads.DELTA_TYPE, AnchorSyncPayloads.DELTA_CODEC);
        registrar.playToClient(AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE, AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_CODEC);
        registrar.playToClient(StatusPayloads.VERSION_TYPE, StatusPayloads.VERSION_CODEC);

        registrar.playToServer(AnchorSyncPayloads.READY_TYPE, AnchorSyncPayloads.READY_CODEC,
                (payload, context) -> NeoForgeAnchorSyncServer.handleReady(context));
        registrar.playToServer(AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_TYPE, AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_CODEC,
                NeoForgeAnchorSyncServer::handleCreateAnchor);
        registrar.playToServer(StatusPayloads.REQUEST_TYPE, StatusPayloads.REQUEST_CODEC,
                (payload, context) -> NeoForgeSignPortStatusNetworking.handleServerVersionRequest(context));
    }
}
