package tech.endorsed.signport.status;

import net.fabricmc.loader.api.FabricLoader;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.internal.modstatus.ModStatusClientState;
import tech.endorsed.signport.internal.modstatus.ModStatusConfig;
import tech.endorsed.signport.internal.modstatus.ModStatusMessages;
import tech.endorsed.signport.internal.modstatus.ModStatusServerStatus;
import tech.endorsed.signport.internal.modstatus.ModStatusVersionPayload;
import tech.endorsed.signport.internal.modstatus.StatusTone;
import tech.endorsed.signport.internal.modstatus.VersionMismatchSeverity;
import tech.endorsed.signport.internal.modstatus.VersionStatus;

public final class SignPortStatus {
    public static final String SERVER_VERSION_CHANNEL_PATH = "status_version";
    public static final int SERVER_DETECTION_GRACE_TICKS = 100;

    private static final String UPDATE_URL = "https://github.com/TnTBass/signport";
    private static volatile ModStatusConfig config;
    private static volatile ModStatusClientState clientState;

    private SignPortStatus() {
    }

    public static ModStatusConfig config() {
        ModStatusConfig current = config;
        if (current != null) {
            return current;
        }
        synchronized (SignPortStatus.class) {
            if (config == null) {
                config = buildConfig();
            }
            return config;
        }
    }

    public static ModStatusClientState createClientState() {
        return ModStatusClientState.create(config());
    }

    public static ModStatusClientState clientState() {
        ModStatusClientState current = clientState;
        if (current != null) {
            return current;
        }
        synchronized (SignPortStatus.class) {
            if (clientState == null) {
                clientState = createClientState();
            }
            return clientState;
        }
    }

    public static void onClientJoin() {
        clientState().unknown();
    }

    public static void onServerStatus(ModStatusServerStatus serverStatus) {
        clientState().connected(serverStatus);
    }

    public static void onClientDisconnect() {
        clientState().disconnected();
    }

    public static byte[] encodeServerStatus() {
        ModStatusConfig current = config();
        return ModStatusVersionPayload.encodeServerStatus(
                current.clientVersion(),
                current.clientBuild(),
                VersionMismatchSeverity.WARN);
    }

    public static ModStatusServerStatus decodeServerStatus(byte[] payload) {
        return ModStatusVersionPayload.decodeServerStatus(payload);
    }

    public static boolean shouldMarkServerNotDetected(boolean playerPresent, boolean statusReceived, int ticksSinceJoin) {
        return playerPresent && !statusReceived && ticksSinceJoin >= SERVER_DETECTION_GRACE_TICKS;
    }

    public static String versionWithBuild(String version, String build) {
        String normalizedVersion = version == null || version.isBlank() ? "Unknown" : version.trim();
        if (build == null || build.isBlank()) {
            return normalizedVersion;
        }
        return normalizedVersion + " (" + build.trim() + ")";
    }

    public static int toneColor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> 0x55AA55;
            case TEAL -> 0x55AAAA;
            case ORANGE -> 0xFFAA00;
            case RED -> 0xFF5555;
            case GRAY -> 0xAAAAAA;
        };
    }

    private static ModStatusConfig buildConfig() {
        return ModStatusConfig.builder()
                .modId(SignPort.MOD_ID)
                .displayName("SignPort")
                .clientVersion(resolveVersion())
                .clientBuild(null)
                .updateUrl(UPDATE_URL)
                .payloadChannel(SignPort.MOD_ID, SERVER_VERSION_CHANNEL_PATH)
                .messages(passiveMessages())
                .build();
    }

    private static String resolveVersion() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(SignPort.MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("Unknown");
        } catch (RuntimeException exception) {
            return "Unknown";
        }
    }

    private static ModStatusMessages passiveMessages() {
        return ModStatusMessages.builder()
                .label(VersionStatus.MATCHED, "Matched")
                .label(VersionStatus.DIFFERENT, "Different versions")
                .label(VersionStatus.DISCONNECTED, "Disconnected")
                .label(VersionStatus.SERVER_NOT_DETECTED, "Server not detected")
                .label(VersionStatus.UNKNOWN, "Unknown")
                .help(VersionStatus.MATCHED, "Client and server SignPort versions match.")
                .help(VersionStatus.DIFFERENT, "Different SignPort versions may affect optional client features.")
                .help(VersionStatus.DISCONNECTED, "Not connected to a server or world.")
                .help(VersionStatus.SERVER_NOT_DETECTED, "No SignPort status payload was detected from this server.")
                .help(VersionStatus.UNKNOWN, "Waiting for the server SignPort status payload.")
                .build();
    }
}
