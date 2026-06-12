package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

    public static Optional<Vec3> resolve(BlockPos anchorPos, SpaceProbe probe) {
        if (anchorPos == null || probe == null) return Optional.empty();

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int yOffset : Y_OFFSETS) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) continue;

                        BlockPos candidate = anchorPos.offset(xOffset, yOffset, zOffset);
                        if (probe.isSafeStandingPosition(candidate)) {
                            return Optional.of(Vec3.atBottomCenterOf(candidate));
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<Vec3> resolve(Level world, BlockPos anchorPos) {
        if (world == null || anchorPos == null) return Optional.empty();
        if (!SignPortConfig.get().safeTeleportSearch()) return Optional.of(Vec3.atBottomCenterOf(anchorPos));

        return resolve(anchorPos, pos -> isSafeStandingPosition(world, pos));
    }

    private static boolean isSafeStandingPosition(Level world, BlockPos pos) {
        if (pos.getY() < world.getMinY() || pos.above().getY() > world.getMaxY()) {
            return false;
        }

        BlockState foot = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState support = world.getBlockState(pos.below());

        return isOpenSafeSpace(world, pos, foot)
                && isOpenSafeSpace(world, pos.above(), head)
                && support.isFaceSturdy(world, pos.below(), Direction.UP)
                && !isHarmful(support);
    }

    private static boolean isOpenSafeSpace(Level world, BlockPos pos, BlockState state) {
        return state.getCollisionShape(world, pos).isEmpty()
                && state.getFluidState().isEmpty()
                && !isHarmful(state);
    }

    private static boolean isHarmful(BlockState state) {
        return state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.getFluidState().is(FluidTags.LAVA);
    }
}
