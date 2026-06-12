package tech.endorsed.signport.network;

import tech.endorsed.signport.internal.modstatus.ModStatusVersionPayload;
import tech.endorsed.signport.internal.modstatus.VersionMismatchSeverity;
import tech.endorsed.signport.status.SignPortStatus;

public final class SignPortStatusNetworking {
    public static final int SERVER_VERSION_REQUEST_INTERVAL_TICKS = 40;
    public static final int NO_SERVER_VERSION_REQUEST_TICK = -1;

    private SignPortStatusNetworking() {
    }

    public static boolean sendConfiguredServerVersionIfSupported(
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
