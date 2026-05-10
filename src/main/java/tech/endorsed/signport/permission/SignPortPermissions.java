package tech.endorsed.signport.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import tech.endorsed.signport.config.SignPortConfig;

public final class SignPortPermissions {
	public static final String ANCHOR_CREATE = "signport.anchor.create";
	public static final String ANCHOR_DELETE = "signport.anchor.delete";
	public static final String ANCHOR_LIST = "signport.anchor.list";
	public static final String TELEPORT_COMMAND = "signport.teleport.command";
	public static final String SIGN_CREATE = "signport.sign.create";
	public static final String SIGN_EDIT = "signport.sign.edit";
	public static final String SIGN_BREAK = "signport.sign.break";
	public static final String SIGN_USE = "signport.sign.use";

	private SignPortPermissions() {
	}

	public static boolean canCreateAnchor(CommandSourceStack source) {
		return Permissions.check(source, ANCHOR_CREATE, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canDeleteAnchor(CommandSourceStack source) {
		return Permissions.check(source, ANCHOR_DELETE, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canListAnchors(CommandSourceStack source) {
		return Permissions.check(source, ANCHOR_LIST, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canUseTeleportCommand(CommandSourceStack source) {
		return Permissions.check(source, TELEPORT_COMMAND, SignPortConfig.get().teleportCommandDefault());
	}

	public static boolean canCreateSign(Player player) {
		return Permissions.check(player, SIGN_CREATE, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canEditSign(Player player) {
		return Permissions.check(player, SIGN_EDIT, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canBreakSign(Player player) {
		return Permissions.check(player, SIGN_BREAK, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canUseSign(Player player) {
		return Permissions.check(player, SIGN_USE, SignPortConfig.get().signUseDefault());
	}
}
