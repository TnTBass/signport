package tech.endorsed.signport.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

public final class SignPortPermissions {
	public static final String ANCHOR_CREATE = "signport.anchor.create";
	public static final String ANCHOR_DELETE = "signport.anchor.delete";
	public static final String ANCHOR_LIST = "signport.anchor.list";
	public static final String TELEPORT_COMMAND = "signport.teleport.command";
	public static final String SIGN_CREATE = "signport.sign.create";
	public static final String SIGN_EDIT = "signport.sign.edit";
	public static final String SIGN_BREAK = "signport.sign.break";
	public static final String SIGN_USE = "signport.sign.use";

	private static final int OP_LEVEL = 2;

	private SignPortPermissions() {
	}

	public static boolean canCreateAnchor(ServerCommandSource source) {
		return Permissions.check(source, ANCHOR_CREATE, OP_LEVEL);
	}

	public static boolean canDeleteAnchor(ServerCommandSource source) {
		return Permissions.check(source, ANCHOR_DELETE, OP_LEVEL);
	}

	public static boolean canListAnchors(ServerCommandSource source) {
		return Permissions.check(source, ANCHOR_LIST, OP_LEVEL);
	}

	public static boolean canUseTeleportCommand(ServerCommandSource source) {
		return Permissions.check(source, TELEPORT_COMMAND, true);
	}

	public static boolean canCreateSign(PlayerEntity player) {
		return Permissions.check(player, SIGN_CREATE, OP_LEVEL);
	}

	public static boolean canEditSign(PlayerEntity player) {
		return Permissions.check(player, SIGN_EDIT, OP_LEVEL);
	}

	public static boolean canBreakSign(PlayerEntity player) {
		return Permissions.check(player, SIGN_BREAK, OP_LEVEL);
	}

	public static boolean canUseSign(PlayerEntity player) {
		return Permissions.check(player, SIGN_USE, true);
	}
}
