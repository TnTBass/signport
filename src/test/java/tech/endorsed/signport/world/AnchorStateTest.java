package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorStateTest {
    // Construct ResourceKey<Level> values directly to avoid triggering Level's
    // static initializer, which requires the Minecraft bootstrap to have run.
    // ResourceKey.create / createRegistryKey are pure data — no bootstrap needed.
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("the_nether"));

    @Test
    void findsAnchorsByExactName() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD);

        state.addAnchor(anchor);

        assertEquals(anchor, state.findAnchor("spawn", OVERWORLD).orElseThrow());
        assertTrue(state.findAnchor("Spawn", OVERWORLD).isEmpty());
    }

    @Test
    void findsAnchorsByCaseInsensitiveNameForPortalSigns() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("Spawn", new BlockPos(1, 64, 2), OVERWORLD);

        state.addAnchor(anchor);

        assertEquals(anchor, state.findAnchorIgnoreCase("spawn", OVERWORLD).orElseThrow());
        assertEquals(anchor, state.findAnchorIgnoreCase("SPAWN", OVERWORLD).orElseThrow());
    }

    @Test
    void updatesLookupAfterDeleteAndClear() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));
        state.addAnchor(new Anchor("nether", new BlockPos(3, 65, 4), NETHER));

        assertTrue(state.deleteAnchor("spawn", OVERWORLD));
        assertTrue(state.findAnchor("spawn", OVERWORLD).isEmpty());
        assertTrue(state.findAnchorIgnoreCase("spawn", OVERWORLD).isEmpty());

        state.clearAnchors(NETHER);

        assertTrue(state.findAnchor("nether", NETHER).isEmpty());
        assertTrue(state.findAnchorIgnoreCase("nether", NETHER).isEmpty());
    }

    @Test
    void anchorNamesAreUniquePerDimension() {
        AnchorState state = new AnchorState();
        Anchor overworld = new Anchor("fortress", new BlockPos(100, 64, 200), OVERWORLD);
        Anchor nether    = new Anchor("fortress", new BlockPos(10, 64, 20), NETHER);

        state.addAnchor(overworld);
        state.addAnchor(nether);

        assertEquals(overworld, state.findAnchorIgnoreCase("fortress", OVERWORLD).orElseThrow());
        assertEquals(nether,    state.findAnchorIgnoreCase("fortress", NETHER).orElseThrow());
        assertEquals(overworld, state.findAnchorIgnoreCase("Fortress", OVERWORLD).orElseThrow());
    }
}
