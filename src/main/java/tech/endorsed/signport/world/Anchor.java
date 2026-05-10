package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;

public class Anchor {
    public String name;
    public BlockPos pos;

    public Anchor(String name, int x, int y, int z)
    {
        this.name = name;
        pos = new BlockPos(x, y, z);
    }

    public Anchor(String name, BlockPos pos)
    {
        this.name = name;
        this.pos = pos;
    }
}
