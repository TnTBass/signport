package tech.endorsed.signport.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import tech.endorsed.signport.config.SignPortConfig;

public final class SignPortPermissions {
	public static final String ANCHOR_CREATE = SignPortPermissionPolicy.ANCHOR_CREATE;
	public static final String ANCHOR_DELETE = SignPortPermissionPolicy.ANCHOR_DELETE;
	public static final String ANCHOR_LIST = SignPortPermissionPolicy.ANCHOR_LIST;
	public static final String TELEPORT_COMMAND = SignPortPermissionPolicy.TELEPORT_COMMAND;
	public static final String SIGN_CREATE = SignPortPermissionPolicy.SIGN_CREATE;
	public static final String SIGN_EDIT = SignPortPermissionPolicy.SIGN_EDIT;
	public static final String SIGN_BREAK = SignPortPermissionPolicy.SIGN_BREAK;
	public static final String SIGN_USE = SignPortPermissionPolicy.SIGN_USE;
	private static volatile Provider provider = Provider.VANILLA;

	private SignPortPermissions() {
	}

	public static void install(Provider implementation) {
		provider = implementation == null ? Provider.VANILLA : implementation;
	}

	public static boolean canCreateAnchor(CommandSourceStack source) {
		return provider.check(source, ANCHOR_CREATE, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canDeleteAnchor(CommandSourceStack source) {
		return provider.check(source, ANCHOR_DELETE, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canListAnchors(CommandSourceStack source) {
		return provider.check(source, ANCHOR_LIST, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canUseTeleportCommand(CommandSourceStack source) {
		return provider.check(source, TELEPORT_COMMAND, SignPortConfig.get().teleportCommandDefault());
	}

	public static boolean canCreateSign(Player player) {
		return provider.check(player, SIGN_CREATE, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canEditSign(Player player) {
		return provider.check(player, SIGN_EDIT, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canBreakSign(Player player) {
		return provider.check(player, SIGN_BREAK, SignPortConfig.get().protectedActionOpLevel());
	}

	public static boolean canUseSign(Player player) {
		return provider.check(player, SIGN_USE, SignPortConfig.get().signUseDefault());
	}

	public interface Provider {
		Provider VANILLA = new Provider() {
			@Override
			public boolean check(CommandSourceStack source, String node, int opLevel) {
				if (source.getEntity() instanceof Player player) {
					return check(player, node, opLevel);
				}
				return hasOpLevel(source.permissions(), opLevel);
			}

			@Override
			public boolean check(CommandSourceStack source, String node, boolean fallback) {
				return fallback || check(source, node, SignPortConfig.get().protectedActionOpLevel());
			}

			@Override
			public boolean check(Player player, String node, int opLevel) {
				return hasOpLevel(player.permissions(), opLevel);
			}

			@Override
			public boolean check(Player player, String node, boolean fallback) {
				return fallback || check(player, node, SignPortConfig.get().protectedActionOpLevel());
			}
		};

		boolean check(CommandSourceStack source, String node, int opLevel);

		boolean check(CommandSourceStack source, String node, boolean fallback);

		boolean check(Player player, String node, int opLevel);

		boolean check(Player player, String node, boolean fallback);
	}

	private static boolean hasOpLevel(PermissionSet permissions, int opLevel) {
		if (permissions instanceof LevelBasedPermissionSet levelBased) {
			return levelBased.level().isEqualOrHigherThan(PermissionLevel.byId(opLevel));
		}
		return permissions == PermissionSet.ALL_PERMISSIONS;
	}
}
