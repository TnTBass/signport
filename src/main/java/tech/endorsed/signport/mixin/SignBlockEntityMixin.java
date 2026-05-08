package tech.endorsed.signport.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.PortSignFormat;
import tech.endorsed.signport.world.PortSignEntity;

import java.util.List;
import java.util.function.UnaryOperator;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {
	@Shadow private SignText frontText;
	@Shadow private SignText backText;

	public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Inject(at = @At("HEAD"), method = "tryChangeText", cancellable = true)
	private void onTryChangeText(PlayerEntity player, boolean front, List<FilteredMessage> messages, CallbackInfo ci) {
		if (this.getWorld() == null || this.getWorld().isClient()) return;

		SignText activeText = front ? this.frontText : this.backText;
		SignText inactiveText = front ? this.backText : this.frontText;
		boolean existingPortalSign = PortSignEntity.isSignPortSign(activeText) || PortSignEntity.isSignPortSign(inactiveText);
		boolean requestedPortalSign = isPortalSignText(messages);

		if (existingPortalSign && !SignPortPermissions.canEditSign(player)) {
			player.sendMessage(Text.literal("You do not have permissions to edit port signs."), true);
			ci.cancel();
			return;
		}

		if (!existingPortalSign && requestedPortalSign && !SignPortPermissions.canCreateSign(player)) {
			player.sendMessage(Text.literal("You do not have permissions to create port signs."), true);
			ci.cancel();
		}
	}

	@Inject(at = @At("RETURN"), method = "changeText")
	private void onSignChange(UnaryOperator<SignText> textChanger, boolean front, CallbackInfoReturnable<Boolean> cir) {
		if (cir.isCancelled()) return;
		if (this.getWorld() == null || this.getWorld().isClient()) return;

		SignText activeText = front ? this.frontText : this.backText;

		Pair<Boolean, Anchor> foundAnchor = PortSignEntity.isValidPortSign(this.getWorld(), activeText);

		PortSignEntity.updatePortLink(activeText, foundAnchor.getLeft());
	}

	private static boolean isPortalSignText(List<FilteredMessage> messages) {
		return messages.size() > 1 && PortSignFormat.isPortalMarker(messages.get(1).raw());
	}
}
