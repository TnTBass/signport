package tech.endorsed.signport.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorState;
import tech.endorsed.signport.world.PortSignEntity;

import java.util.function.UnaryOperator;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {
	@Shadow private SignText frontText;
	@Shadow private SignText backText;

	public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// FIXME: Any player can create a linked sign
	@Inject(at = @At("RETURN"), method = "changeText")
	private void onSignChange(UnaryOperator<SignText> textChanger, boolean front, CallbackInfoReturnable<Boolean> cir) {
		if (cir.isCancelled()) return;
		if (this.getWorld() == null || this.getWorld().isClient()) return;

		SignText activeText = front ? this.frontText : this.backText;

		Pair<Boolean, Anchor> foundAnchor = PortSignEntity.isValidPortSign(this.getWorld(), activeText);

		PortSignEntity.updatePortLink(activeText, foundAnchor.getLeft());
	}
}