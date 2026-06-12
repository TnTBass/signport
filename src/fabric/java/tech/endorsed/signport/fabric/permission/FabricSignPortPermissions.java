package tech.endorsed.signport.fabric.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import tech.endorsed.signport.permission.SignPortPermissions;

public final class FabricSignPortPermissions implements SignPortPermissions.Provider {
    @Override
    public boolean check(CommandSourceStack source, String node, int opLevel) {
        return Permissions.check(source, node, opLevel);
    }

    @Override
    public boolean check(CommandSourceStack source, String node, boolean fallback) {
        return Permissions.check(source, node, fallback);
    }

    @Override
    public boolean check(Player player, String node, int opLevel) {
        return Permissions.check(player, node, opLevel);
    }

    @Override
    public boolean check(Player player, String node, boolean fallback) {
        return Permissions.check(player, node, fallback);
    }
}
