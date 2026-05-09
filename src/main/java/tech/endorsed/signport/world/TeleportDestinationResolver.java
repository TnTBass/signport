package tech.endorsed.signport.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import tech.endorsed.signport.config.SignPortConfig;

import java.util.Optional;

public final class TeleportDestinationResolver {
    private static final int SEARCH_RADIUS = 3;
    private static final int[] Y_OFFSETS = {0, 1, -1, 2, -2};

    private TeleportDestinationResolver() {
    }

    @FunctionalInterface
    public interface SpaceProbe {
        boolean isSafeStandingPosition(BlockPos pos);
    }

    public static Optional<Vec3d> resolve(BlockPos anchorPos, SpaceProbe probe) {
        if (anchorPos == null || probe == null) return Optional.empty();

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int yOffset : Y_OFFSETS) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) continue;

                        BlockPos candidate = anchorPos.add(xOffset, yOffset, zOffset);
                        if (probe.isSafeStandingPosition(candidate)) {
                            return Optional.of(Vec3d.ofBottomCenter(candidate));
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<Vec3d> resolve(World world, BlockPos anchorPos) {
        if (world == null || anchorPos == null) return Optional.empty();
        if (!SignPortConfig.get().safeTeleportSearch()) return Optional.of(Vec3d.ofBottomCenter(anchorPos));

        return resolve(anchorPos, pos -> isSafeStandingPosition(world, pos));
    }

    private static boolean isSafeStandingPosition(World world, BlockPos pos) {
        if (pos.getY() < world.getBottomY() || pos.up().getY() > world.getTopYInclusive()) {
            return false;
        }

        BlockState foot = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        BlockState support = world.getBlockState(pos.down());

        return isOpenSafeSpace(world, pos, foot)
                && isOpenSafeSpace(world, pos.up(), head)
                && support.isSideSolidFullSquare(world, pos.down(), Direction.UP)
                && !isHarmful(support);
    }

    private static boolean isOpenSafeSpace(World world, BlockPos pos, BlockState state) {
        return !state.isSolidBlock(world, pos)
                && state.getCollisionShape(world, pos).isEmpty()
                && state.getFluidState().isEmpty()
                && !isHarmful(state);
    }

    private static boolean isHarmful(BlockState state) {
        return state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.CAMPFIRE)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.SOUL_CAMPFIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.WITHER_ROSE)
                || state.getFluidState().isIn(FluidTags.LAVA);
    }
}
