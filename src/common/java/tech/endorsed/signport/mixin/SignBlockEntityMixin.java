package tech.endorsed.signport.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
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

	@Inject(at = @At("HEAD"), method = "updateSignText", cancellable = true)
	private void onTryChangeText(Player player, boolean front, List<FilteredText> messages, CallbackInfo ci) {
		if (this.getLevel() == null || this.getLevel().isClientSide()) return;

		SignText activeText = front ? this.frontText : this.backText;
		SignText inactiveText = front ? this.backText : this.frontText;
		boolean existingPortalSign = PortSignEntity.isSignPortSign(activeText) || PortSignEntity.isSignPortSign(inactiveText);
		boolean requestedPortalSign = isPortalSignText(messages);

		if (existingPortalSign && !SignPortPermissions.canEditSign(player)) {
			player.sendOverlayMessage(Component.literal("You do not have permissions to edit port signs."));
			ci.cancel();
			return;
		}

		if (!existingPortalSign && requestedPortalSign && !SignPortPermissions.canCreateSign(player)) {
			player.sendOverlayMessage(Component.literal("You do not have permissions to create port signs."));
			ci.cancel();
		}
	}

	@Inject(at = @At("RETURN"), method = "updateText")
	private void onSignChange(UnaryOperator<SignText> textChanger, boolean front, CallbackInfoReturnable<Boolean> cir) {
		if (cir.isCancelled()) return;
		if (this.getLevel() == null || this.getLevel().isClientSide()) return;

		SignText activeText = front ? this.frontText : this.backText;

		boolean foundAnchor = PortSignEntity.isValidPortSign(this.getLevel(), activeText);

		PortSignEntity.updatePortLink(activeText, foundAnchor);
	}

	private static boolean isPortalSignText(List<FilteredText> messages) {
		return messages.size() > 1 && PortSignFormat.isPortalMarker(messages.get(1).raw());
	}
}
