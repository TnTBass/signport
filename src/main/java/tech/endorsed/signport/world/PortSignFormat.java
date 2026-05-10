package tech.endorsed.signport.world;

import net.minecraft.resources.Identifier;

import java.util.Locale;

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

		// Accept common shorthand names in addition to full identifiers.
		// "the_nether", "the_end", and "overworld" already work via the
		// default minecraft: namespace, but "nether" and "end" do not.
		return switch (line.toLowerCase(Locale.ROOT)) {
			case "nether" -> Identifier.fromNamespaceAndPath("minecraft", "the_nether");
			case "end"    -> Identifier.fromNamespaceAndPath("minecraft", "the_end");
			default       -> Identifier.tryParse(line); // preserve case for custom dimensions
		};
	}
}
