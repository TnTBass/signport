package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class Anchor {
    public String name;
    public BlockPos pos;
    public ResourceKey<Level> dimension;

    public Anchor(String name, BlockPos pos, ResourceKey<Level> dimension) {
        this.name = name;
        this.pos = pos;
        this.dimension = dimension;
    }
}
