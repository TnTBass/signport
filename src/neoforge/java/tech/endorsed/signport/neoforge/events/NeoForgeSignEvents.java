package tech.endorsed.signport.neoforge.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.PortSignEntity;

public final class NeoForgeSignEvents {
    private NeoForgeSignEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeSignEvents::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(NeoForgeSignEvents::onUseItemOnBlock);
    }

    static void onBreakBlock(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (SignPortPermissions.canBreakSign(player)) return;

        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level) || level.isClientSide()) return;

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof SignBlockEntity sign)) return;

        SignText front = sign.getFrontText();
        SignText back = sign.getBackText();
        if (!PortSignEntity.isSignPortSign(front) && !PortSignEntity.isSignPortSign(back)) return;

        player.sendOverlayMessage(Component.literal("You do not have permissions to remove port signs."));
        event.setCanceled(true);
        event.setNotifyClient(true);
    }

    static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof SignBlockEntity sign)) return;
        if (SignPortPermissions.canEditSign(player) && player.hasPose(Pose.CROUCHING)) {
            return;
        }

        SignText primaryText = sign.getText(sign.isFacingFrontText(player));
        SignText secondaryText = sign.isFacingFrontText(player) ? sign.getBackText() : sign.getFrontText();
        PortSignEntity.PortalDestination destination =
                PortSignEntity.resolvePortalDestination(level, primaryText, secondaryText);

        if (destination.valid() && !SignPortPermissions.canUseSign(player)) {
            player.sendOverlayMessage(Component.literal("You do not have permissions to use port signs."));
            event.cancelWithResult(InteractionResult.FAIL);
            return;
        }

        if (!PortSignEntity.teleportToDestination(player, destination, primaryText, secondaryText)) {
            return;
        }

        event.cancelWithResult(InteractionResult.SUCCESS);
    }
}
