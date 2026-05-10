package tech.endorsed.signport.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import tech.endorsed.signport.config.SignPortConfig;

import java.util.EnumSet;
import java.util.Optional;

public class PortSignEntity {
    public record PortalDestination(boolean valid, Anchor anchor, Level world, SignText text) {
    }

    public static boolean teleportToDestination(ServerPlayer entity, Level world, SignText activeText) {
        return teleportToDestination(entity, world, activeText, null);
    }

    public static boolean teleportToDestination(ServerPlayer entity, Level world, SignText primaryText, SignText secondaryText) {
        return teleportToDestination(entity, resolvePortalDestination(world, primaryText, secondaryText), primaryText, secondaryText);
    }

    public static boolean teleportToDestination(
            ServerPlayer entity,
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

    private static void teleportToAnchor(ServerPlayer entity, PortalDestination destination) {
        if (destination.world() == null) return;

        Anchor anchor = destination.anchor();
        Optional<Vec3> resolvedPosition = TeleportDestinationResolver.resolve(destination.world(), anchor.pos);
        if (resolvedPosition.isEmpty()) {
            entity.sendSystemMessage(Component.literal("Could not find a safe destination near anchor '%s'".formatted(anchor.name)));
            return;
        }

        Vec3 pos = resolvedPosition.get();
        entity.teleportTo((ServerLevel) destination.world(),
                pos.x,
                pos.y,
                pos.z,
                EnumSet.noneOf(Relative.class),
                entity.getYRot(),
                entity.getXRot(),
                false);
    }

    public static boolean isSignPortSign(SignText activeText) {
        if (activeText == null) return false;

        return PortSignFormat.isPortalMarker(activeText.getMessage(1, false).getString());
    }

    public static boolean isValidPortSign(Level world, SignText activeText) {
        return resolvePortalDestination(world, activeText).valid();
    }

    public static PortalDestination resolvePortalDestination(Level world, SignText activeText) {
        if (world == null || world.isClientSide()) return new PortalDestination(false, null, world, activeText);

        if (!isSignPortSign(activeText)) return new PortalDestination(false, null, world, activeText);

        String line2 = PortSignFormat.normalizeLine(activeText.getMessage(2, false).getString());

        Optional<AnchorState> stateOpt = AnchorState.peekServerState(world.getServer());
        if (stateOpt.isEmpty()) return new PortalDestination(false, null, world, activeText);

        // Explicit line 3 dimension takes priority — checked before the current-dimension
        // lookup so a sign with "the_nether" always goes to the nether anchor, even if an
        // anchor with the same name exists in the current dimension.
        ServerLevel targetWorld = (ServerLevel) world;
        ResourceKey<Level> targetDimension = world.dimension();

        if (SignPortConfig.get().crossDimensionPortalSigns()) {
            var dimensionId = PortSignFormat.parseDimensionId(activeText.getMessage(3, false).getString());
            if (dimensionId != null) {
                ServerLevel specifiedWorld = world.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
                if (specifiedWorld != null) {
                    targetWorld = specifiedWorld;
                    targetDimension = specifiedWorld.dimension();
                }
            }
        }

        return stateOpt.get().findAnchorIgnoreCase(line2, targetDimension)
                .map(anchor -> new PortalDestination(true, anchor, targetWorld, activeText))
                .orElse(new PortalDestination(false, null, world, activeText));
    }

    public static PortalDestination resolvePortalDestination(Level world, SignText primaryText, SignText secondaryText) {
        PortalDestination destination = resolvePortalDestination(world, primaryText);
        if (destination.valid() || secondaryText == null || secondaryText == primaryText) {
            return destination;
        }

        return resolvePortalDestination(world, secondaryText);
    }

    public static void updatePortLink(SignText activeText, boolean foundAnchor) {
        MutableComponent text = (MutableComponent) activeText.getMessage(1, false);
        if (foundAnchor) {
            text.setStyle(text.getStyle().withColor(0x2FDD48));
        } else if (isSignPortSign(activeText)) {
            text.setStyle(text.getStyle().withColor(0xFF0000));
        }
    }
}
