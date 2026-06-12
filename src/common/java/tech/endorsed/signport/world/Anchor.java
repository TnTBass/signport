package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class Anchor {
    public String name;
    public BlockPos pos;
    public ResourceKey<Level> dimension;
    public String group;
    public long createdAt;

    public Anchor(String name, BlockPos pos, ResourceKey<Level> dimension) {
        this(name, pos, dimension, "");
    }

    public Anchor(String name, BlockPos pos, ResourceKey<Level> dimension, String group) {
        this(name, pos, dimension, group, 0L);
    }

    public Anchor(String name, BlockPos pos, ResourceKey<Level> dimension, String group, long createdAt) {
        this.name = name;
        this.pos = pos;
        this.dimension = dimension;
        this.group = group == null ? "" : group;
        this.createdAt = createdAt;
    }

    public String group() {
        return group;
    }

    public long createdAt() {
        return createdAt;
    }
}
