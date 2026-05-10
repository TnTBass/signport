package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorStateTest {
    @Test
    void findsAnchorsByExactName() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("spawn", new BlockPos(1, 64, 2));

        state.addAnchor(anchor);

        assertEquals(anchor, state.findAnchor("spawn").orElseThrow());
        assertTrue(state.findAnchor("Spawn").isEmpty());
    }

    @Test
    void findsAnchorsByCaseInsensitiveNameForPortalSigns() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("Spawn", new BlockPos(1, 64, 2));

        state.addAnchor(anchor);

        assertEquals(anchor, state.findAnchorIgnoreCase("spawn").orElseThrow());
        assertEquals(anchor, state.findAnchorIgnoreCase("SPAWN").orElseThrow());
    }

    @Test
    void updatesLookupAfterDeleteAndClear() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2)));
        state.addAnchor(new Anchor("nether", new BlockPos(3, 65, 4)));

        assertTrue(state.deleteAnchor("spawn"));
        assertTrue(state.findAnchor("spawn").isEmpty());
        assertTrue(state.findAnchorIgnoreCase("spawn").isEmpty());

        state.clearAnchors();

        assertTrue(state.findAnchor("nether").isEmpty());
        assertTrue(state.findAnchorIgnoreCase("nether").isEmpty());
    }
}
