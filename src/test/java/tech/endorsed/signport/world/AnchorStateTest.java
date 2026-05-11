package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void filtersAnchorsByGroupWithinDimension() {
        AnchorState state = new AnchorState();
        Anchor shop = new Anchor("diamond", new BlockPos(1, 64, 2), OVERWORLD, "shops");
        Anchor base = new Anchor("north", new BlockPos(3, 65, 4), OVERWORLD, "bases");
        Anchor netherShop = new Anchor("quartz", new BlockPos(5, 66, 6), NETHER, "shops");
        Anchor ungrouped = new Anchor("lobby", new BlockPos(7, 67, 8), OVERWORLD);

        state.addAnchor(shop);
        state.addAnchor(base);
        state.addAnchor(netherShop);
        state.addAnchor(ungrouped);

        assertEquals(List.of(shop), state.getAnchorsByGroup(OVERWORLD, "shops"));
        assertEquals(List.of(ungrouped), state.getAnchorsByGroup(OVERWORLD, ""));
    }

    @Test
    void groupsForDimensionAreDistinctSortedAndSkipUngrouped() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("diamond", new BlockPos(1, 64, 2), OVERWORLD, "shops"));
        state.addAnchor(new Anchor("emerald", new BlockPos(3, 65, 4), OVERWORLD, "shops"));
        state.addAnchor(new Anchor("north", new BlockPos(5, 66, 6), OVERWORLD, "bases"));
        state.addAnchor(new Anchor("lobby", new BlockPos(7, 67, 8), OVERWORLD));
        state.addAnchor(new Anchor("quartz", new BlockPos(9, 68, 10), NETHER, "nether"));

        assertEquals(List.of("bases", "shops"), List.copyOf(state.getGroupsForDimension(OVERWORLD)));
    }

    @Test
    void setAnchorGroupUpdatesExistingAnchorWithoutChangingName() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("diamond", new BlockPos(1, 64, 2), OVERWORLD, "shops", 1234L);
        state.addAnchor(anchor);

        assertTrue(state.setAnchorGroup("diamond", OVERWORLD, "bases"));

        Anchor moved = state.findAnchor("diamond", OVERWORLD).orElseThrow();
        assertEquals("diamond", moved.name);
        assertEquals("bases", moved.group);
        assertEquals(1234L, moved.createdAt);
    }

    @Test
    void codecReadsPhaseOneAnchorsWithoutGroupAsUngrouped() {
        AnchorState state = AnchorState.CODEC.parse(NbtOps.INSTANCE, stateTag(anchorTag("spawn", OVERWORLD, null)))
                .result()
                .orElseThrow();

        Anchor anchor = state.findAnchor("spawn", OVERWORLD).orElseThrow();
        assertEquals("", anchor.group);
        assertEquals(0L, anchor.createdAt);
    }

    @Test
    void codecRoundTripsAnchorsWithGroup() {
        AnchorState original = new AnchorState();
        original.addAnchor(new Anchor("diamond", new BlockPos(1, 64, 2), OVERWORLD, "shops", 1234L));

        var encoded = AnchorState.CODEC.encodeStart(NbtOps.INSTANCE, original).result().orElseThrow();
        AnchorState decoded = AnchorState.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals("shops", decoded.findAnchor("diamond", OVERWORLD).orElseThrow().group);
        assertEquals(1234L, decoded.findAnchor("diamond", OVERWORLD).orElseThrow().createdAt);
        assertTrue(encoded.toString().contains("group"));
        assertTrue(encoded.toString().contains("createdAt"));
    }

    @Test
    void addAnchorStampsCreatedAtWhenMissing() {
        AnchorState state = new AnchorState();
        Anchor anchor = new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD);

        state.addAnchor(anchor);

        assertTrue(state.findAnchor("spawn", OVERWORLD).orElseThrow().createdAt > 0L);
    }

    private static CompoundTag stateTag(CompoundTag... anchors) {
        CompoundTag state = new CompoundTag();
        ListTag anchorList = new ListTag();
        for (CompoundTag anchor : anchors) {
            anchorList.add(anchor);
        }
        state.put("v2_anchors", anchorList);
        return state;
    }

    private static CompoundTag anchorTag(String name, ResourceKey<Level> dimension, String group) {
        CompoundTag anchor = new CompoundTag();
        anchor.putString("name", name);
        anchor.put("pos", posTag(1, 64, 2));
        anchor.putString("dimension", dimension.identifier().toString());
        if (group != null) {
            anchor.putString("group", group);
        }
        return anchor;
    }

    private static CompoundTag posTag(int x, int y, int z) {
        CompoundTag pos = new CompoundTag();
        pos.putInt("xPos", x);
        pos.putInt("yPos", y);
        pos.putInt("zPos", z);
        return pos;
    }
}
