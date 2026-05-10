package tech.endorsed.signport.command;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.world.Anchor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorListViewTest {
    // Construct ResourceKey<Level> directly to avoid Level's static initializer.
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("overworld"));

    private static Anchor anchor(String name) {
        return new Anchor(name, new BlockPos(0, 64, 0), OVERWORLD);
    }

    private static Anchor anchor(String name, String group) {
        return new Anchor(name, new BlockPos(0, 64, 0), OVERWORLD, group);
    }

    @Test
    void filterEmptyReturnsInput() {
        List<Anchor> input = List.of(anchor("spawn"), anchor("shop"));
        assertEquals(input, AnchorListView.filter(input, ""));
        assertEquals(input, AnchorListView.filter(input, null));
    }

    @Test
    void filterIsCaseInsensitiveSubstring() {
        List<Anchor> input = List.of(anchor("Spawn"), anchor("ShopMain"), anchor("nether-hub"));
        List<Anchor> matches = AnchorListView.filter(input, "shop");
        assertEquals(1, matches.size());
        assertEquals("ShopMain", matches.get(0).name);
    }

    @Test
    void totalPagesRoundsUpAndIsAtLeastOne() {
        assertEquals(1, AnchorListView.totalPages(0, 10));
        assertEquals(1, AnchorListView.totalPages(10, 10));
        assertEquals(2, AnchorListView.totalPages(11, 10));
        assertEquals(3, AnchorListView.totalPages(25, 10));
    }

    @Test
    void clampPageBoundsToValidRange() {
        assertEquals(1, AnchorListView.clampPage(0, 5));
        assertEquals(1, AnchorListView.clampPage(-3, 5));
        assertEquals(5, AnchorListView.clampPage(99, 5));
        assertEquals(3, AnchorListView.clampPage(3, 5));
    }

    @Test
    void sliceReturnsRequestedPageWindow() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7);
        assertEquals(List.of(1, 2, 3), AnchorListView.slice(items, 1, 3));
        assertEquals(List.of(4, 5, 6), AnchorListView.slice(items, 2, 3));
        assertEquals(List.of(7), AnchorListView.slice(items, 3, 3));
    }

    @Test
    void sliceOutOfRangeIsEmpty() {
        List<Integer> items = List.of(1, 2, 3);
        assertTrue(AnchorListView.slice(items, 5, 3).isEmpty());
    }

    @Test
    void sortByGroupThenNamePlacesUngroupedLast() {
        List<Anchor> sorted = AnchorListView.sortByGroupThenName(List.of(
                anchor("lobby"),
                anchor("zoo", "shops"),
                anchor("north", "bases"),
                anchor("diamond", "shops")));

        assertEquals(List.of("north", "diamond", "zoo", "lobby"),
                sorted.stream().map(a -> a.name).toList());
    }
}
