package tech.endorsed.signport.world;

import net.minecraft.block.entity.SignText;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import tech.endorsed.signport.config.SignPortConfig;

import java.util.EnumSet;
import java.util.Optional;

public class PortSignEntity {
    public record PortalDestination(boolean valid, Anchor anchor, World world, SignText text) {
    }

    public static boolean teleportToDestination(ServerPlayerEntity entity, World world, SignText activeText) {
        return teleportToDestination(entity, world, activeText, null);
    }

    public static boolean teleportToDestination(ServerPlayerEntity entity, World world, SignText primaryText, SignText secondaryText) {
        return teleportToDestination(entity, resolvePortalDestination(world, primaryText, secondaryText), primaryText, secondaryText);
    }

    public static boolean teleportToDestination(
            ServerPlayerEntity entity,
            PortalDestination destination,
            SignText primaryText,
            SignText secondaryText
    ) {
        if (destination.valid()) {
            updatePortLink(destination.text(), true);
            teleportToAnchor(entity, destination);
            return true;
        }

        updatePortLink(primaryText, false);
        if (secondaryText != null && secondaryText != primaryText) {
            updatePortLink(secondaryText, false);
        }

        return false;
    }

    private static void teleportToAnchor(ServerPlayerEntity entity, PortalDestination destination) {
        if (destination.world() == null) return;

        Anchor anchor = destination.anchor();
        Optional<Vec3d> resolvedPosition = TeleportDestinationResolver.resolve(destination.world(), anchor.pos);
        if (resolvedPosition.isEmpty()) {
            entity.sendMessage(Text.literal("Could not find a safe destination near anchor '%s'".formatted(anchor.name)));
            return;
        }

        Vec3d pos = resolvedPosition.get();
        entity.teleport((ServerWorld) destination.world(),
                pos.x,
                pos.y,
                pos.z,
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
        PortalDestination portSign = resolvePortalDestination(world, activeText);
        return new Pair<>(portSign.valid(), portSign.anchor());
    }

    public static PortalDestination resolvePortalDestination(World world, SignText activeText) {
        if  (world == null || world.isClient()) return new PortalDestination(false, null, world, activeText);

        if (!isSignPortSign(activeText)) return new PortalDestination(false, null, world, activeText);

        String line2 = PortSignFormat.normalizeLine(activeText.getMessage(2, false).getString());

        AnchorState state = AnchorState.getServerState((ServerWorld) world);
        if (state == null) return new PortalDestination(false, null, world, activeText);

        Optional<Anchor> anchor = state.findAnchorIgnoreCase(line2);
        if (anchor.isPresent()) {
            return new PortalDestination(true, anchor.get(), world, activeText);
        }

        if (!SignPortConfig.get().crossDimensionPortalSigns()) {
            return new PortalDestination(false, null, world, activeText);
        }

        var dimensionId = PortSignFormat.parseDimensionId(activeText.getMessage(3, false).getString());
        if (dimensionId == null) return new PortalDestination(false, null, world, activeText);

        ServerWorld dimensionWorld = world.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (dimensionWorld == null) return new PortalDestination(false, null, world, activeText);

        AnchorState dimensionalAnchorState = AnchorState.getServerState(dimensionWorld);
        if (dimensionalAnchorState == null) return new PortalDestination(false, null, world, activeText);

        Optional<Anchor> dimensionalAnchor = dimensionalAnchorState.findAnchorIgnoreCase(line2);
        if (dimensionalAnchor.isPresent()) {
            return new PortalDestination(true, dimensionalAnchor.get(), dimensionWorld, activeText);
        }

        return new PortalDestination(false, null, world, activeText);
    }

    public static PortalDestination resolvePortalDestination(World world, SignText primaryText, SignText secondaryText) {
        PortalDestination destination = resolvePortalDestination(world, primaryText);
        if (destination.valid() || secondaryText == null || secondaryText == primaryText) {
            return destination;
        }

        return resolvePortalDestination(world, secondaryText);
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
