package tech.endorsed.signport.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.endorsed.signport.world.PortSignEntity;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow protected ServerWorld world;

    @Inject(at = @At("HEAD"), method = "interactBlock", cancellable = true)
    public void onInteract(ServerPlayerEntity player,
                           World world,
                           ItemStack stack,
                           Hand hand,
                           BlockHitResult hitResult,
                           CallbackInfoReturnable<ActionResult> cir) {
        BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
        if (!(blockEntity instanceof SignBlockEntity sign)) return;
        if (player.hasPermissionLevel(2) && player.isInPose(EntityPose.CROUCHING)) {
            return;
        }

        SignText primaryText = sign.getText(sign.isPlayerFacingFront(player));
        SignText secondaryText = sign.isPlayerFacingFront(player) ? sign.getBackText() : sign.getFrontText();

        // If neither side teleports, the sign can be edited normally.
        if (!PortSignEntity.teleportToDestination(player, world, primaryText, secondaryText)) {
            return;
        }

        cir.setReturnValue(ActionResult.PASS);
        cir.cancel();
    }
}
