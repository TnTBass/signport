package tech.endorsed.signport.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.client.AnchorClient;
import tech.endorsed.signport.network.AnchorSyncPayloads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorBrowserScreenTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> THE_END = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("the_end"));

    @Test
    void rowTitleUsesAnchorNameWithoutRepeatingVisibleGroupHeader() {
        AnchorClient anchor = new AnchorClient("DesertVillage389", new BlockPos(889, 68, 8798), OVERWORLD,
                "FarLands", 1234L);

        assertEquals("DesertVillage389", AnchorBrowserScreen.rowTitle(anchor));
    }

    @Test
    void rowClickCommandUsesDimensionQualifiedSignPortTeleportWithoutExecute() {
        AnchorClient anchor = new AnchorClient("EndBase", new BlockPos(12, 70, -31), THE_END, "", 1234L);

        assertEquals("sp tp \"EndBase\" minecraft:the_end", AnchorBrowserScreen.rowClickCommand(anchor));
    }

    @Test
    void rowClickCommandQuotesAnchorNamesWithSpaces() {
        AnchorClient anchor = new AnchorClient("End Base", new BlockPos(12, 70, -31), THE_END, "", 1234L);

        assertEquals("sp tp \"End Base\" minecraft:the_end", AnchorBrowserScreen.rowClickCommand(anchor));
    }

    @Test
    void browserTeleportActionsUseTeleportPermissionRatherThanDeletePermission() {
        var teleportOnly = new AnchorSyncPayloads.PermissionSnapshot(false, false, false, false, false, true);
        var deleteOnly = new AnchorSyncPayloads.PermissionSnapshot(false, false, false, true, false, false);

        assertTrue(AnchorBrowserScreen.canActivateTeleport(teleportOnly));
        assertFalse(AnchorBrowserScreen.canActivateTeleport(deleteOnly));
        assertEquals(
                "Permission denied: requires signport.teleport.command",
                AnchorBrowserScreen.teleportPermissionTooltip());
    }

    @Test
    void maxScrollKeepsRowsAbovePanelBottomPadding() {
        int panelHeight = AnchorBrowserScreen.panelHeightForContent(240, 220);

        assertEquals(124, AnchorBrowserScreen.maxScrollForContent(220, panelHeight));
    }
}
