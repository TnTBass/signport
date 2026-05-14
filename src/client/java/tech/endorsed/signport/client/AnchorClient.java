package tech.endorsed.signport.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.network.AnchorSyncPayloads;

public record AnchorClient(
        String name,
        BlockPos pos,
        ResourceKey<Level> dimension,
        String group,
        long createdAt
) {
    public static AnchorClient from(AnchorSyncPayloads.SyncedAnchor anchor) {
        return new AnchorClient(anchor.name(), anchor.pos(), anchor.dimension(), anchor.group(), anchor.createdAt());
    }

    public String displayName() {
        if (group == null || group.isBlank()) return name;
        return group + "/" + name;
    }
}
