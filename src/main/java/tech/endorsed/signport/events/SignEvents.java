package tech.endorsed.signport.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.PortSignEntity;

public class SignEvents implements PlayerBlockBreakEvents.Before {
    @Override
    public boolean beforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof SignBlockEntity)) return true;
        if (SignPortPermissions.canBreakSign(player)) return true;

        SignText front = ((SignBlockEntity) blockEntity).getFrontText();
        SignText back = ((SignBlockEntity) blockEntity).getBackText();
        if (!PortSignEntity.isSignPortSign(front) && !PortSignEntity.isSignPortSign(back)) return true;

        player.sendOverlayMessage(Component.literal("You do not have permissions to remove port signs."));

        return false;
    }
}
