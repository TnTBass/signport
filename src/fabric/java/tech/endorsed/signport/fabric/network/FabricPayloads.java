package tech.endorsed.signport.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import tech.endorsed.signport.network.AnchorSyncPayloads;
import tech.endorsed.signport.network.StatusPayloads;

import java.util.concurrent.atomic.AtomicBoolean;

public final class FabricPayloads {
    private static final AtomicBoolean clientboundRegistered = new AtomicBoolean();
    private static final AtomicBoolean serverboundRegistered = new AtomicBoolean();

    private FabricPayloads() {
    }

    public static void registerClientbound() {
        if (!clientboundRegistered.compareAndSet(false, true)) return;
        PayloadTypeRegistry.clientboundPlay().register(AnchorSyncPayloads.FULL_TYPE, AnchorSyncPayloads.FULL_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AnchorSyncPayloads.DELTA_TYPE, AnchorSyncPayloads.DELTA_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE, AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatusPayloads.VERSION_TYPE, StatusPayloads.VERSION_CODEC);
    }

    public static void registerServerbound() {
        if (!serverboundRegistered.compareAndSet(false, true)) return;
        PayloadTypeRegistry.serverboundPlay().register(AnchorSyncPayloads.READY_TYPE, AnchorSyncPayloads.READY_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_TYPE, AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StatusPayloads.REQUEST_TYPE, StatusPayloads.REQUEST_CODEC);
    }
}
