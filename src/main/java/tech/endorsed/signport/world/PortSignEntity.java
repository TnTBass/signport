package tech.endorsed.signport.world;

import net.minecraft.block.entity.SignText;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Pair;
import net.minecraft.world.World;
import oshi.util.tuples.Triplet;
import tech.endorsed.signport.SignPort;

import java.util.EnumSet;

public class PortSignEntity {
    public static boolean teleportToDestination(ServerPlayerEntity entity, World world, SignText activeText) {
        return teleportToDestination(entity, world, activeText, null);
    }

    public static boolean teleportToDestination(ServerPlayerEntity entity, World world, SignText primaryText, SignText secondaryText) {
        Triplet<Boolean, Anchor, World> foundAnchor = isValidPortSignWorld(world, primaryText);
        if (foundAnchor.getA()) {
            updatePortLink(primaryText, true);
            teleportToAnchor(entity, foundAnchor);
            return true;
        }

        if (secondaryText != null && secondaryText != primaryText) {
            foundAnchor = isValidPortSignWorld(world, secondaryText);
            if (foundAnchor.getA()) {
                updatePortLink(secondaryText, true);
                teleportToAnchor(entity, foundAnchor);
                return true;
            }
        }

        updatePortLink(primaryText, false);
        if (secondaryText != null && secondaryText != primaryText) {
            updatePortLink(secondaryText, false);
        }

        return false;
    }

    private static void teleportToAnchor(ServerPlayerEntity entity, Triplet<Boolean, Anchor, World> foundAnchor) {
        if (foundAnchor.getC() == null) return;

        Anchor anchor = foundAnchor.getB();
        entity.teleport((ServerWorld) foundAnchor.getC(),
                anchor.pos.getX(),
                anchor.pos.getY(),
                anchor.pos.getZ(),
                EnumSet.noneOf(PositionFlag.class),
                entity.getYaw(),
                entity.getPitch(),
                false);
    }

    public static boolean isSignPortSign(SignText activeText) {
        if (activeText == null) return false;

        return PortSignFormat.isPortalMarker(activeText.getMessage(1, false).getString());
    }

    public static Pair<Boolean, Anchor> isValidPortSign(World world, SignText activeText) {
        Triplet<Boolean, Anchor, World> portSign = isValidPortSignWorld(world, activeText);
        return new Pair<>(portSign.getA(), portSign.getB());
    }

    public static Triplet<Boolean, Anchor, World> isValidPortSignWorld(World world, SignText activeText) {
        if  (world == null || world.isClient()) return new Triplet<>(false, null, world);

        if (!isSignPortSign(activeText)) return new Triplet<>(false, null, world);

        String line2 = PortSignFormat.normalizeLine(activeText.getMessage(2, false).getString());

        AnchorState state = AnchorState.getServerState((ServerWorld) world);
        if (state == null) return new Triplet<>(false, null, world);

        for (Anchor anchor: state.GetAnchors()) {
            if (line2.equalsIgnoreCase(anchor.name)) {
                return new Triplet<>(true, anchor, world);
            }
        }

        var dimensionId = PortSignFormat.parseDimensionId(activeText.getMessage(3, false).getString());
        if (dimensionId == null) return new Triplet<>(false, null, world);

        ServerWorld dimensionWorld = world.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (dimensionWorld == null) return new Triplet<>(false, null, world);

        // Checking for interdimensional teleports
        AnchorState dimensionalAnchorState = AnchorState.getServerState(dimensionWorld);
        if (dimensionalAnchorState == null) return new Triplet<>(false, null, world);

        for (Anchor anchor: dimensionalAnchorState.GetAnchors()) {
            if (line2.equalsIgnoreCase(anchor.name)) {
                // Found interdimensional teleport
                return new Triplet<>(true, anchor, dimensionWorld);
            }
        }

        return new Triplet<>(false, null, world);
    }

    public static void updatePortLink(SignText activeText, boolean foundAnchor) {
        MutableText text = (MutableText) activeText.getMessage(1, false);
        if (foundAnchor) {
            text.setStyle(text.getStyle().withColor(0x2FDD48));
        } else if (isSignPortSign(activeText)) {
            text.setStyle(text.getStyle().withColor(0xFF0000));
        }
    }
}
