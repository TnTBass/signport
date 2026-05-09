package tech.endorsed.signport.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportDestinationResolverTest {
    @Test
    void centersSafeAnchorPosition() {
        BlockPos anchor = new BlockPos(10, 64, -3);

        var destination = TeleportDestinationResolver.resolve(anchor, anchor::equals);

        assertTrue(destination.isPresent());
        assertEquals(new Vec3d(10.5, 64.0, -2.5), destination.get());
    }

    @Test
    void searchesNearbyWhenAnchorPositionIsUnsafe() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        BlockPos safe = new BlockPos(-1, 64, -1);

        var destination = TeleportDestinationResolver.resolve(anchor, safe::equals);

        assertTrue(destination.isPresent());
        assertEquals(Vec3d.ofBottomCenter(safe), destination.get());
    }

    @Test
    void prefersSameColumnOneBlockUpBeforeHorizontalSearch() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Set<BlockPos> safePositions = Set.of(
                new BlockPos(-1, 64, -1),
                new BlockPos(0, 65, 0)
        );

        var destination = TeleportDestinationResolver.resolve(anchor, safePositions::contains);

        assertTrue(destination.isPresent());
        assertEquals(Vec3d.ofBottomCenter(new BlockPos(0, 65, 0)), destination.get());
    }

    @Test
    void returnsEmptyWhenNoSafePositionIsNearby() {
        BlockPos anchor = new BlockPos(0, 64, 0);

        var destination = TeleportDestinationResolver.resolve(anchor, pos -> false);

        assertTrue(destination.isEmpty());
    }
}
