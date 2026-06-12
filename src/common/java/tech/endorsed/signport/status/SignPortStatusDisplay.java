package tech.endorsed.signport.status;

import java.util.ArrayList;
import java.util.List;
import tech.endorsed.signport.internal.modstatus.ModStatusDisplay;
import tech.endorsed.signport.internal.modstatus.StatusTone;

/**
 * SignPort-owned display helpers for rendering MSK status data in client UI.
 */
public final class SignPortStatusDisplay {
    public static final String STATUS_SQUARE = "\u25A0";
    static final int STATUS_SQUARE_SIZE = 8;
    public static final int STATUS_SQUARE_BORDER_COLOR = 0xFF222222;

    private SignPortStatusDisplay() {
    }

    public static int toneColor(StatusTone tone) {
        return switch (tone) {
            case GREEN -> 0xFF55FF55;
            case TEAL -> 0xFF55FFFF;
            case ORANGE -> 0xFFFFAA00;
            case RED -> 0xFFFF5555;
            case GRAY -> 0xFFAAAAAA;
        };
    }

    static int toneRgb(StatusTone tone) {
        return toneColor(tone) & 0x00FFFFFF;
    }

    public static List<String> tooltipText(ModStatusDisplay display) {
        List<String> lines = new ArrayList<>();
        lines.add(display.displayName());
        lines.add("Status: " + display.statusLabel());
        lines.add("Client: " + versionWithBuild(display.clientVersion(), display.clientBuild()));
        lines.add("Server: " + versionWithBuild(display.serverVersion(), display.serverBuild()));
        String helpText = display.helpText();
        if (helpText != null && !helpText.isEmpty()) {
            lines.add(helpText);
        }
        return lines;
    }

    public static String versionWithBuild(String version, String build) {
        if (version == null || version.isBlank()) {
            return "Unknown";
        }
        String normalizedVersion = version.trim();
        return build == null || build.isBlank() || "dev".equalsIgnoreCase(build.trim())
                ? normalizedVersion
                : normalizedVersion + "+" + build.trim();
    }
}
