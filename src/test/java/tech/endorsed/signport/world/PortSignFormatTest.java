package tech.endorsed.signport.world;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortSignFormatTest {
	@Test
	void acceptsPortalMarkersIgnoringCaseAndWhitespace() {
		assertTrue(PortSignFormat.isPortalMarker("[sp]"));
		assertTrue(PortSignFormat.isPortalMarker(" [SP] "));
		assertTrue(PortSignFormat.isPortalMarker("[signport]"));
		assertTrue(PortSignFormat.isPortalMarker(" [SignPort] "));
	}

	@Test
	void rejectsNonPortalMarkers() {
		assertFalse(PortSignFormat.isPortalMarker(null));
		assertFalse(PortSignFormat.isPortalMarker(""));
		assertFalse(PortSignFormat.isPortalMarker("[portal]"));
		assertFalse(PortSignFormat.isPortalMarker("signport"));
	}

	@Test
	void parsesValidDimensionIds() {
		assertEquals(Identifier.of("minecraft", "the_nether"), PortSignFormat.parseDimensionId("minecraft:the_nether"));
		assertEquals(Identifier.of("minecraft", "overworld"), PortSignFormat.parseDimensionId(" overworld "));
	}

	@Test
	void ignoresBlankOrInvalidDimensionIds() {
		assertNull(PortSignFormat.parseDimensionId(null));
		assertNull(PortSignFormat.parseDimensionId(""));
		assertNull(PortSignFormat.parseDimensionId(" "));
		assertNull(PortSignFormat.parseDimensionId("Minecraft:The_Nether"));
		assertNull(PortSignFormat.parseDimensionId("not a dimension"));
	}
}
