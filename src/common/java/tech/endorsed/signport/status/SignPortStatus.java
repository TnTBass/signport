package tech.endorsed.signport.status;

import tech.endorsed.signport.BuildInfo;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.internal.modstatus.ModStatusClientState;
import tech.endorsed.signport.internal.modstatus.ModStatusConfig;
import tech.endorsed.signport.internal.modstatus.ModStatusMessages;
import tech.endorsed.signport.internal.modstatus.ModStatusServerStatus;
import tech.endorsed.signport.internal.modstatus.ModStatusVersionPayload;
import tech.endorsed.signport.internal.modstatus.StatusTone;
import tech.endorsed.signport.internal.modstatus.VersionMismatchSeverity;
import tech.endorsed.signport.internal.modstatus.VersionStatus;

import java.util.function.Supplier;

public final class SignPortStatus {
    public static final String SERVER_VERSION_CHANNEL_PATH = "server_version";
    public static final int SERVER_DETECTION_GRACE_TICKS = 100;

    private static final String UPDATE_URL = "https://github.com/TnTBass/signport";
    private static volatile Supplier<String> versionSupplier = () -> "Unknown";
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

    public static void installVersionSupplier(Supplier<String> supplier) {
        versionSupplier = supplier == null ? () -> "Unknown" : supplier;
        synchronized (SignPortStatus.class) {
            config = null;
            clientState = null;
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
        return SignPortStatusDisplay.versionWithBuild(version, build);
    }

    public static int toneColor(StatusTone tone) {
        return SignPortStatusDisplay.toneRgb(tone);
    }

    private static ModStatusConfig buildConfig() {
        return ModStatusConfig.builder()
                .modId(SignPort.MOD_ID)
                .displayName("SignPort")
                .clientVersion(resolveVersion())
                .clientBuild(BuildInfo.BUILD_NUMBER)
                .updateUrl(UPDATE_URL)
                .payloadChannel(SignPort.MOD_ID, SERVER_VERSION_CHANNEL_PATH)
                .messages(passiveMessages())
                .build();
    }

    private static String resolveVersion() {
        try {
            String version = versionSupplier.get();
            return version == null || version.isBlank() ? "Unknown" : version;
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
