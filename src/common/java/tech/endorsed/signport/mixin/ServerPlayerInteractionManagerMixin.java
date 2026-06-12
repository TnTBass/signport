package tech.endorsed.signport.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.PortSignEntity;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow protected ServerLevel level;

    @Inject(at = @At("HEAD"), method = "useItemOn", cancellable = true)
    public void onInteract(ServerPlayer player,
                           Level world,
                           ItemStack stack,
                           InteractionHand hand,
                           BlockHitResult hitResult,
                           CallbackInfoReturnable<InteractionResult> cir) {
        BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
        if (!(blockEntity instanceof SignBlockEntity sign)) return;
        if (SignPortPermissions.canEditSign(player) && player.hasPose(Pose.CROUCHING)) {
            return;
        }

        SignText primaryText = sign.getText(sign.isFacingFrontText(player));
        SignText secondaryText = sign.isFacingFrontText(player) ? sign.getBackText() : sign.getFrontText();
        PortSignEntity.PortalDestination destination = PortSignEntity.resolvePortalDestination(world, primaryText, secondaryText);

        if (destination.valid() && !SignPortPermissions.canUseSign(player)) {
            player.sendOverlayMessage(Component.literal("You do not have permissions to use port signs."));
            cir.setReturnValue(InteractionResult.FAIL);
            cir.cancel();
            return;
        }

        // If neither side teleports, the sign can be edited normally.
        if (!PortSignEntity.teleportToDestination(player, destination, primaryText, secondaryText)) {
            return;
        }

        cir.setReturnValue(InteractionResult.PASS);
        cir.cancel();
    }
}
