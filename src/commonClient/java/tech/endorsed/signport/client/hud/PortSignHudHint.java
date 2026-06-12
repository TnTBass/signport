package tech.endorsed.signport.client.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tech.endorsed.signport.client.AnchorClient;
import tech.endorsed.signport.client.SignPortClientState;
import tech.endorsed.signport.client.config.SignPortClientConfig;
import tech.endorsed.signport.world.PortSignFormat;

import java.util.Optional;

public final class PortSignHudHint {
    private static LookKey lastLookKey;
    private static ResolvedHint cachedHint;

    private PortSignHudHint() {
    }

    public static void tick(Minecraft client) {
        if (!SignPortClientConfig.get().hudHintEnabled || !SignPortClientState.serverHasSignPort()) {
            clear();
            return;
        }
        if (client.player == null || client.level == null || client.hitResult == null) {
            clear();
            return;
        }
        if (client.hitResult.getType() != HitResult.Type.BLOCK || !(client.hitResult instanceof BlockHitResult hit)) {
            clear();
            return;
        }
        if (!(client.level.getBlockEntity(hit.getBlockPos()) instanceof SignBlockEntity sign)) {
            clear();
            return;
        }

        SignText text = sign.getText(sign.isFacingFrontText(client.player));
        if (!PortSignFormat.isPortalMarker(text.getMessage(1, false).getString())) {
            clear();
            return;
        }

        LookKey lookKey = new LookKey(
                hit.getBlockPos(),
                text.getMessage(1, false).getString(),
                text.getMessage(2, false).getString(),
                text.getMessage(3, false).getString(),
                client.level.dimension());
        if (!lookKey.equals(lastLookKey)) {
            lastLookKey = lookKey;
            cachedHint = resolve(lookKey);
        }

        if (cachedHint != null) {
            client.player.sendOverlayMessage(cachedHint.message(lookKey.currentDimension(), client));
        }
    }

    private static ResolvedHint resolve(LookKey lookKey) {
        String anchorName = PortSignFormat.normalizeLine(lookKey.anchorLine());
        Identifier explicitId = PortSignFormat.parseDimensionId(lookKey.dimensionLine());
        ResourceKey<Level> explicitDimension = explicitId == null
                ? null
                : ResourceKey.create(Registries.DIMENSION, explicitId);
        ResourceKey<Level> lookupDimension = explicitDimension == null ? lookKey.currentDimension() : explicitDimension;

        Optional<AnchorClient> exact = SignPortClientState.find(anchorName, lookupDimension);
        if (exact.isPresent()) {
            return new ResolvedHint(ChatFormatting.GREEN, exact.get(), anchorName, null);
        }

        if (explicitDimension != null) {
            Optional<AnchorClient> any = SignPortClientState.findAnyDimension(anchorName);
            if (any.isPresent()) {
                return new ResolvedHint(ChatFormatting.YELLOW, any.get(), anchorName, null);
            }
        }

        String dimension = explicitId == null ? lookKey.currentDimension().identifier().toString() : explicitId.toString();
        return new ResolvedHint(ChatFormatting.RED, null, anchorName, dimension);
    }

    private static void clear() {
        lastLookKey = null;
        cachedHint = null;
    }

    private record LookKey(
            BlockPos pos,
            String markerLine,
            String anchorLine,
            String dimensionLine,
            ResourceKey<Level> currentDimension
    ) {
    }

    private record ResolvedHint(ChatFormatting dotColor, AnchorClient anchor, String anchorName, String missingDimension) {
        Component message(ResourceKey<Level> currentDimension, Minecraft client) {
            if (anchor == null) {
                return Component.literal("●").withStyle(dotColor)
                        .append(Component.literal(" → " + anchorName + " · " + missingDimension));
            }

            MutableComponent message = Component.literal("●").withStyle(dotColor)
                    .append(Component.literal(" → " + anchor.displayName() + " · " + anchor.dimension().identifier()));
            if (anchor.dimension().equals(currentDimension) && client.player != null) {
                long distance = Math.round(Math.sqrt(anchor.pos().distSqr(client.player.blockPosition())));
                message.append(Component.literal(" · " + distance + "m"));
            }
            return message;
        }
    }
}
