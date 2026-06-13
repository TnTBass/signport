package tech.endorsed.signport.neoforge.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.permission.SignPortPermissionPolicy;
import tech.endorsed.signport.permission.SignPortPermissions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NeoForgeSignPortPermissions implements SignPortPermissions.Provider {
    private static final Map<String, PermissionNode<Boolean>> NODES_BY_NAME = createNodesByName();
    private static final List<PermissionNode<Boolean>> NODES = List.copyOf(NODES_BY_NAME.values());

    public static List<PermissionNode<Boolean>> nodes() {
        return NODES;
    }

    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES.toArray(PermissionNode[]::new));
    }

    @Override
    public boolean check(CommandSourceStack source, String node, int opLevel) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return check(player, node, opLevel);
        }
        return SignPortPermissions.Provider.VANILLA.check(source, node, opLevel);
    }

    @Override
    public boolean check(CommandSourceStack source, String node, boolean fallback) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return check(player, node, fallback);
        }
        return SignPortPermissions.Provider.VANILLA.check(source, node, fallback);
    }

    @Override
    public boolean check(Player player, String node, int opLevel) {
        if (player instanceof ServerPlayer serverPlayer) {
            PermissionNode<Boolean> permissionNode = NODES_BY_NAME.get(node);
            if (permissionNode != null) {
                return PermissionAPI.getPermission(serverPlayer, permissionNode);
            }
        }
        return SignPortPermissions.Provider.VANILLA.check(player, node, opLevel);
    }

    @Override
    public boolean check(Player player, String node, boolean fallback) {
        if (player instanceof ServerPlayer serverPlayer) {
            PermissionNode<Boolean> permissionNode = NODES_BY_NAME.get(node);
            if (permissionNode != null) {
                return PermissionAPI.getPermission(serverPlayer, permissionNode);
            }
        }
        return SignPortPermissions.Provider.VANILLA.check(player, node, fallback);
    }

    private static Map<String, PermissionNode<Boolean>> createNodesByName() {
        Map<String, PermissionNode<Boolean>> nodes = new LinkedHashMap<>();
        SignPortPermissionPolicy.defaultsByNode().forEach((node, defaultSource) -> {
            nodes.put(node, new PermissionNode<>(
                    SignPort.MOD_ID,
                    nodePath(node),
                    PermissionTypes.BOOLEAN,
                    (player, ignored, context) -> resolveDefault(node, defaultSource, player)));
        });
        return Collections.unmodifiableMap(nodes);
    }

    static String nodePath(String node) {
        String prefix = SignPort.MOD_ID + ".";
        if (!node.startsWith(prefix)) {
            throw new IllegalStateException("Permission node '" + node + "' must start with '" + prefix + "'");
        }
        return node.substring(prefix.length());
    }

    private static boolean resolveDefault(
            String node,
            SignPortPermissionPolicy.DefaultSource defaultSource,
            ServerPlayer player) {
        SignPortPermissionPolicy.PermissionDefault permissionDefault =
                defaultSource.resolve(SignPortConfig.get());
        if (permissionDefault.fallback() != null) {
            // Boolean defaults are explicit everyone/vanilla-fallback policy:
            // true grants the node by default, false still allows configured op fallback.
            return permissionDefault.fallback()
                    || SignPortPermissions.Provider.VANILLA.check(player, node, SignPortConfig.get().protectedActionOpLevel());
        }
        return SignPortPermissions.Provider.VANILLA.check(player, node, permissionDefault.opLevel());
    }
}
