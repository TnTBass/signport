package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorStateTest {
    @Test
    void findsAnchorsByExactName() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("spawn", new BlockPos(1, 64, 2), Level.OVERWORLD);

        state.addAnchor(anchor);

        assertEquals(anchor, state.findAnchor("spawn", Level.OVERWORLD).orElseThrow());
        assertTrue(state.findAnchor("Spawn", Level.OVERWORLD).isEmpty());
    }

    @Test
    void findsAnchorsByCaseInsensitiveNameForPortalSigns() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("Spawn", new BlockPos(1, 64, 2), Level.OVERWORLD);

        state.addAnchor(anchor);

        assertEquals(anchor, state.findAnchorIgnoreCase("spawn", Level.OVERWORLD).orElseThrow());
        assertEquals(anchor, state.findAnchorIgnoreCase("SPAWN", Level.OVERWORLD).orElseThrow());
    }

    @Test
    void updatesLookupAfterDeleteAndClear() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), Level.OVERWORLD));
        state.addAnchor(new Anchor("nether", new BlockPos(3, 65, 4), Level.NETHER));

        assertTrue(state.deleteAnchor("spawn", Level.OVERWORLD));
        assertTrue(state.findAnchor("spawn", Level.OVERWORLD).isEmpty());
        assertTrue(state.findAnchorIgnoreCase("spawn", Level.OVERWORLD).isEmpty());

        state.clearAnchors(Level.NETHER);

        assertTrue(state.findAnchor("nether", Level.NETHER).isEmpty());
        assertTrue(state.findAnchorIgnoreCase("nether", Level.NETHER).isEmpty());
    }

    @Test
    void anchorNamesAreUniquePerDimension() {
        AnchorState state = new AnchorState();
        Anchor overworld = new Anchor("fortress", new BlockPos(100, 64, 200), Level.OVERWORLD);
        Anchor nether    = new Anchor("fortress", new BlockPos(10, 64, 20), Level.NETHER);

        state.addAnchor(overworld);
        state.addAnchor(nether);

        assertEquals(overworld, state.findAnchorIgnoreCase("fortress", Level.OVERWORLD).orElseThrow());
        assertEquals(nether,    state.findAnchorIgnoreCase("fortress", Level.NETHER).orElseThrow());
        assertEquals(overworld, state.findAnchorIgnoreCase("Fortress", Level.OVERWORLD).orElseThrow());
    }
}
