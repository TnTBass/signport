package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorCreationTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(
                    ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
                    Identifier.withDefaultNamespace("the_nether"));

    @Test
    void createsAnchorAtSuppliedPositionAndDimension() {
        AnchorState state = new AnchorState();

        AnchorCreation.Result result = AnchorCreation.create(
                state, "spawn", new BlockPos(1, 64, 2), OVERWORLD, "bases");

        assertTrue(result.success());
        Anchor anchor = state.findAnchor("spawn", OVERWORLD).orElseThrow();
        assertEquals(new BlockPos(1, 64, 2), anchor.pos);
        assertEquals(OVERWORLD, anchor.dimension);
        assertEquals("bases", anchor.group);
        assertEquals(AnchorCreation.Error.NONE, result.error());
    }

    @Test
    void rejectsBlankName() {
        AnchorCreation.Result result = AnchorCreation.create(
                new AnchorState(), " ", new BlockPos(1, 64, 2), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.INVALID_NAME, result.error());
    }

    @Test
    void rejectsNamesLongerThanGuiSignLineLimit() {
        String tooLong = "a".repeat(AnchorCreation.MAX_ANCHOR_NAME_LENGTH + 1);

        AnchorCreation.Result result = AnchorCreation.create(
                new AnchorState(), tooLong, new BlockPos(1, 64, 2), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.INVALID_NAME, result.error());
    }

    @Test
    void rejectsDuplicateNameInSameDimension() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));

        AnchorCreation.Result result = AnchorCreation.create(
                state, "spawn", new BlockPos(3, 64, 4), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.NAME_CLASH, result.error());
    }

    @Test
    void allowsSameNameInDifferentDimension() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));

        AnchorCreation.Result result = AnchorCreation.create(
                state, "spawn", new BlockPos(3, 64, 4), NETHER, "");

        assertTrue(result.success());
        assertTrue(state.findAnchor("spawn", NETHER).isPresent());
    }

    @Test
    void rejectsDuplicatePositionInSameDimension() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));

        AnchorCreation.Result result = AnchorCreation.create(
                state, "shop", new BlockPos(1, 64, 2), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.POSITION_CLASH, result.error());
    }

    @Test
    void groupStringsMatchExistingServerBehavior() {
        assertEquals("", AnchorCreation.normalizeGroup(null));
        assertEquals("", AnchorCreation.normalizeGroup("-"));
        assertEquals("Spawn", AnchorCreation.normalizeGroup("Spawn"));
        assertEquals("spawns", AnchorCreation.normalizeGroup("spawns"));
    }
}
