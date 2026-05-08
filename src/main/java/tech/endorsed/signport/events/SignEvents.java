package tech.endorsed.signport.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.PortSignEntity;

public class SignEvents implements PlayerBlockBreakEvents.Before {
    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof SignBlockEntity)) return true;
        if (SignPortPermissions.canBreakSign(player)) return true;

        SignText front = ((SignBlockEntity) blockEntity).getFrontText();
        SignText back = ((SignBlockEntity) blockEntity).getBackText();
        Pair<Boolean, Anchor> validPortSign = PortSignEntity.isValidPortSign(world, front);
        if (!validPortSign.getLeft()) {
            validPortSign = PortSignEntity.isValidPortSign(world, back);
            if (!validPortSign.getLeft()) return true;
        }

        player.sendMessage(Text.literal("You do not have permissions to remove port signs."), true);

        return false;
    }
}
