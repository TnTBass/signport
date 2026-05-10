package tech.endorsed.signport.world;

import net.minecraft.resources.Identifier;

public final class PortSignFormat {
	public static final String SHORT_MARKER = "[sp]";
	public static final String LONG_MARKER = "[signport]";

	private PortSignFormat() {
	}

	public static boolean isPortalMarker(String value) {
		String line = normalizeLine(value);
		return line.equalsIgnoreCase(SHORT_MARKER) || line.equalsIgnoreCase(LONG_MARKER);
	}

	public static String normalizeLine(String value) {
		return value == null ? "" : value.trim();
	}

	public static Identifier parseDimensionId(String value) {
		String line = normalizeLine(value);
		if (line.isBlank()) return null;

		return Identifier.tryParse(line);
	}
}
