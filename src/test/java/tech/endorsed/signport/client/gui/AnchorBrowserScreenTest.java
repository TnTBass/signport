package tech.endorsed.signport.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.client.AnchorClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnchorBrowserScreenTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension")),
            Identifier.withDefaultNamespace("overworld"));

    @Test
    void rowTitleUsesAnchorNameWithoutRepeatingVisibleGroupHeader() {
        AnchorClient anchor = new AnchorClient("DesertVillage389", new BlockPos(889, 68, 8798), OVERWORLD,
                "FarLands", 1234L);

        assertEquals("DesertVillage389", AnchorBrowserScreen.rowTitle(anchor));
    }
}
