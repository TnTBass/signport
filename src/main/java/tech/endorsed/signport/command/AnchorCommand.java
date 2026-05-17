package tech.endorsed.signport.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.network.AnchorSyncServer;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorCreation;
import tech.endorsed.signport.world.AnchorState;
import tech.endorsed.signport.world.TeleportDestinationResolver;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.minecraft.commands.Commands.literal;

public class AnchorCommand {
    private static final SimpleCommandExceptionType CREATE_FAILED_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.create.failed"));
    private static final SimpleCommandExceptionType NAME_CLASH_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.create.nameclash"));
    private static final SimpleCommandExceptionType UNKNOWN_NAME_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.delete.unknownname"));
    private static final String CLEAR_GROUP_SENTINEL = "-";
    private enum AnchorSort {
        NAME("name"),
        DISTANCE("distance"),
        RECENT("recent");

        private final String flagValue;

        AnchorSort(String flagValue) {
            this.flagValue = flagValue;
        }

        String flag() {
            return "--sort=" + flagValue;
        }
    }

    /** Suggests anchor names in the player's current dimension. */
    private static final SuggestionProvider<CommandSourceStack> ANCHOR_NAME_SUGGESTIONS = (context, builder) -> {
        var source = context.getSource();
        var server = source.getServer();
        var dim = source.getLevel().dimension();
        List<String> names = AnchorState.peekServerState(server)
                .map(s -> s.getAnchorsForDimension(dim).stream().map(a -> a.name).toList())
                .orElse(List.of());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    /** Suggests existing group names in the player's current dimension, plus '-' to clear a group. */
    private static final SuggestionProvider<CommandSourceStack> GROUP_SUGGESTIONS = (context, builder) -> {
        var source = context.getSource();
        var server = source.getServer();
        var dim = source.getLevel().dimension();
        List<String> groups = AnchorState.peekServerState(server)
                .map(s -> {
                    var suggestions = new java.util.ArrayList<>(s.getGroupsForDimension(dim));
                    suggestions.add(CLEAR_GROUP_SENTINEL);
                    return List.copyOf(suggestions);
                })
                .orElse(List.of(CLEAR_GROUP_SENTINEL));
        return SharedSuggestionProvider.suggest(groups, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(
                literal("signport")
                        .then(literal("tp")
                            .requires(SignPortPermissions::canUseTeleportCommand)
                            .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(ANCHOR_NAME_SUGGESTIONS)
                                .executes(context -> teleportAnchor(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(literal("anchor")
                                .then(literal("list")
                                        .requires(SignPortPermissions::canListAnchors)
                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), "", 1, AnchorSort.NAME))
                                        .then(sortLiteral("--sort=name", AnchorSort.NAME, "", 1))
                                        .then(sortLiteral("--sort=distance", AnchorSort.DISTANCE, "", 1))
                                        .then(sortLiteral("--sort=recent", AnchorSort.RECENT, "", 1))
                                        // /sp anchor list <page> — numeric arg is treated as page
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> AnchorCommand.listAnchors(
                                                        context.getSource(), "", IntegerArgumentType.getInteger(context, "page"), AnchorSort.NAME))
                                                .then(literal("--sort=name")
                                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), "", IntegerArgumentType.getInteger(context, "page"), AnchorSort.NAME)))
                                                .then(literal("--sort=distance")
                                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), "", IntegerArgumentType.getInteger(context, "page"), AnchorSort.DISTANCE)))
                                                .then(literal("--sort=recent")
                                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), "", IntegerArgumentType.getInteger(context, "page"), AnchorSort.RECENT))))
                                        // /sp anchor list <filter> [page] — non-numeric arg is treated as substring filter
                                        .then(Commands.argument("filter", StringArgumentType.word())
                                                .executes(context -> AnchorCommand.listAnchors(
                                                        context.getSource(), StringArgumentType.getString(context, "filter"), 1, AnchorSort.NAME))
                                                .then(literal("--sort=name")
                                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), StringArgumentType.getString(context, "filter"), 1, AnchorSort.NAME)))
                                                .then(literal("--sort=distance")
                                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), StringArgumentType.getString(context, "filter"), 1, AnchorSort.DISTANCE)))
                                                .then(literal("--sort=recent")
                                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), StringArgumentType.getString(context, "filter"), 1, AnchorSort.RECENT)))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(context -> AnchorCommand.listAnchors(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "filter"),
                                                                IntegerArgumentType.getInteger(context, "page"), AnchorSort.NAME))
                                                        .then(literal("--sort=name")
                                                                .executes(context -> AnchorCommand.listAnchors(context.getSource(), StringArgumentType.getString(context, "filter"), IntegerArgumentType.getInteger(context, "page"), AnchorSort.NAME)))
                                                        .then(literal("--sort=distance")
                                                                .executes(context -> AnchorCommand.listAnchors(context.getSource(), StringArgumentType.getString(context, "filter"), IntegerArgumentType.getInteger(context, "page"), AnchorSort.DISTANCE)))
                                                        .then(literal("--sort=recent")
                                                                .executes(context -> AnchorCommand.listAnchors(context.getSource(), StringArgumentType.getString(context, "filter"), IntegerArgumentType.getInteger(context, "page"), AnchorSort.RECENT))))))
                                .then(literal("near")
                                        .requires(SignPortPermissions::canListAnchors)
                                        .executes(context -> AnchorCommand.nearAnchors(
                                                context.getSource(), SignPortConfig.get().defaultNearRadius(), 1))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                                .executes(context -> AnchorCommand.nearAnchors(
                                                        context.getSource(), IntegerArgumentType.getInteger(context, "radius"), 1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(context -> AnchorCommand.nearAnchors(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "radius"),
                                                                IntegerArgumentType.getInteger(context, "page"))))))
                                .then(literal("delete")
                                        .requires(SignPortPermissions::canDeleteAnchor)
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests(ANCHOR_NAME_SUGGESTIONS)
                                                .executes(context -> AnchorCommand.deleteAnchor(context.getSource(), StringArgumentType.getString(context, "name")))))
                                .then(literal("create")
                                        .requires(SignPortPermissions::canCreateAnchor)
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(context -> AnchorCommand.createAnchor(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "name"),
                                                        null,
                                                        ""))
                                                .then(Commands.argument("group", StringArgumentType.word())
                                                        .suggests(GROUP_SUGGESTIONS)
                                                        .executes(context -> AnchorCommand.createAnchor(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "name"),
                                                                null,
                                                                StringArgumentType.getString(context, "group"))))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(context -> AnchorCommand.createAnchor(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "name"),
                                                                BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                                "")))))
                                .then(literal("setgroup")
                                        .requires(SignPortPermissions::canCreateAnchor)
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests(ANCHOR_NAME_SUGGESTIONS)
                                                .then(Commands.argument("group", StringArgumentType.string())
                                                        .suggests(GROUP_SUGGESTIONS)
                                                        .executes(context -> AnchorCommand.setGroup(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "name"),
                                                                StringArgumentType.getString(context, "group"))))))));

        dispatcher.register(literal("sp")
                .redirect(literalCommandNode));
    }

    private static int teleportAnchor(CommandSourceStack source, String name) {
        var player = source.getPlayer();
        if (player == null) return 0;

        var world = source.getLevel();

        Optional<Anchor> anchor = AnchorState.peekServerState(source.getServer())
                .flatMap(s -> s.findAnchor(name, world.dimension()));
        if (anchor.isPresent()) {
            Optional<Vec3> destination = TeleportDestinationResolver.resolve(world, anchor.get().pos);
            if (destination.isEmpty()) {
                player.sendSystemMessage(Component.literal("Could not find a safe destination near anchor '%s'".formatted(anchor.get().name)));
                return 0;
            }

            Vec3 pos = destination.get();
            player.teleportTo(world,
                    pos.x,
                    pos.y,
                    pos.z,
                    EnumSet.noneOf(Relative.class),
                    player.getYRot(),
                    player.getXRot(),
                    false);
            return 1;
        }

        player.sendSystemMessage(Component.literal("Could not find anchor '%s'".formatted(name)));
        return 0;
    }

    public static int createAnchor(CommandSourceStack source, String name, BlockPos pos, String group) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        AnchorState anchorState = AnchorState.getServerState(source.getServer());

        var aPos = pos == null ? source.getPlayer().blockPosition() : pos;
        AnchorCreation.Result result = AnchorCreation.create(anchorState, name, aPos, dim, group);
        if (!result.success()) {
            if (result.error() == AnchorCreation.Error.NAME_CLASH) throw NAME_CLASH_EXCEPTION.create();
            throw CREATE_FAILED_EXCEPTION.create();
        }

        Anchor anchor = result.anchor();
        String normalizedGroup = anchor.group;
        BlueMapIntegration.anchorCreated(source.getServer(), anchor);
        AnchorSyncServer.anchorCreated(source.getServer(), anchor);

        if (normalizedGroup.isEmpty()) {
            player.sendSystemMessage(Component.literal("Created anchor '%s'".formatted(anchor.name)));
        } else {
            player.sendSystemMessage(Component.literal("Created anchor '%s' in group '%s'".formatted(anchor.name, normalizedGroup)));
        }

        return 1;
    }

    public static int setGroup(CommandSourceStack source, String name, String group) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        AnchorState anchorState = AnchorState.getServerState(source.getServer());
        String normalizedGroup = normalizeGroup(group);

        if (!anchorState.setAnchorGroup(name, dim, normalizedGroup)) {
            throw UNKNOWN_NAME_EXCEPTION.create();
        }
        anchorState.findAnchor(name, dim).ifPresent(anchor -> {
            BlueMapIntegration.anchorUpdated(source.getServer(), anchor);
            AnchorSyncServer.anchorUpdated(source.getServer(), anchor);
        });

        if (normalizedGroup.isEmpty()) {
            player.sendSystemMessage(Component.literal("Moved anchor '%s' to (ungrouped)".formatted(name)));
        } else {
            player.sendSystemMessage(Component.literal("Moved anchor '%s' to group '%s'".formatted(name, normalizedGroup)));
        }
        return 1;
    }

    public static int deleteAnchor(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        AnchorState anchorState = AnchorState.getServerState(source.getServer());

        Optional<Anchor> deleted = anchorState.findAnchor(name, dim);
        if (anchorState.deleteAnchor(name, dim)) {
            deleted.ifPresent(anchor -> {
                BlueMapIntegration.anchorDeleted(source.getServer(), dim, anchor.name);
                AnchorSyncServer.anchorDeleted(source.getServer(), anchor.name, dim);
            });
            player.sendSystemMessage(Component.literal("Deleted anchor '%s'".formatted(name)));
            return 1;
        }

        if (name.equalsIgnoreCase("all")) {
            anchorState.clearAnchors(dim);
            BlueMapIntegration.anchorsCleared(source.getServer(), dim);
            AnchorSyncServer.sendFullToAll(source.getServer());
            player.sendSystemMessage(Component.literal("Deleted ALL anchors"));
            return 1;
        }

        throw UNKNOWN_NAME_EXCEPTION.create();
    }

    public static int listAnchors(CommandSourceStack source, String filter, int requestedPage) {
        return listAnchors(source, filter, requestedPage, AnchorSort.NAME);
    }

    private static int listAnchors(CommandSourceStack source, String filter, int requestedPage, AnchorSort sort) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        List<Anchor> dimAnchors = AnchorState.peekServerState(source.getServer())
                .map(s -> s.getAnchorsForDimension(dim))
                .orElse(List.of());
        List<Anchor> matched = sortAnchors(AnchorListView.filter(dimAnchors, filter), sort, player.blockPosition());

        if (matched.isEmpty()) {
            if (filter == null || filter.isEmpty()) {
                player.sendSystemMessage(Component.literal("No anchors exist"));
            } else {
                player.sendSystemMessage(Component.literal("No anchors match '%s'".formatted(filter)));
            }
            return 1;
        }

        return sendAnchorPage(player, matched, requestedPage, filter, sort, false, 0);
    }

    public static int nearAnchors(CommandSourceStack source, int radius, int requestedPage) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        BlockPos origin = player.blockPosition();
        double radiusSquared = (double) radius * radius;
        List<Anchor> matched = AnchorState.peekServerState(source.getServer())
                .map(s -> s.getAnchorsForDimension(source.getLevel().dimension()))
                .orElse(List.of())
                .stream()
                .filter(anchor -> anchor.pos.distSqr(origin) <= radiusSquared)
                .toList();
        matched = AnchorListView.groupByFirstOccurrence(AnchorListView.sortByDistance(matched, origin));

        if (matched.isEmpty()) {
            player.sendSystemMessage(Component.literal("No anchors within %d blocks".formatted(radius)));
            return 1;
        }

        return sendAnchorPage(player, matched, requestedPage, "", AnchorSort.DISTANCE, true, radius);
    }

    private static int sendAnchorPage(
            ServerPlayer player,
            List<Anchor> matched,
            int requestedPage,
            String filter,
            AnchorSort sort,
            boolean near,
            int radius) {
        int pageSize = SignPortConfig.get().anchorListPageSize();
        int totalPages = AnchorListView.totalPages(matched.size(), pageSize);
        int page = AnchorListView.clampPage(requestedPage, totalPages);
        List<Anchor> pageAnchors = AnchorListView.slice(matched, page, pageSize);

        boolean canTeleport = player.permissions() instanceof LevelBasedPermissionSet pls
                && pls.level().isEqualOrHigherThan(PermissionLevel.byId(SignPortConfig.get().protectedActionOpLevel()));

        int startIndex = (page - 1) * pageSize + 1;
        Map<String, Integer> groupCounts = groupCounts(matched);
        String lastGroup = null;
        for (int i = 0; i < pageAnchors.size(); i++) {
            Anchor anchor = pageAnchors.get(i);
            String group = normalizeGroup(anchor.group);
            if (!group.equals(lastGroup)) {
                player.sendSystemMessage(Component.literal("%s (%d)"
                        .formatted(groupLabel(group), groupCounts.getOrDefault(group, 0))));
                lastGroup = group;
            }

            MutableComponent message = anchorRow(startIndex + i, anchor, player.blockPosition(), sort == AnchorSort.DISTANCE);

            if (canTeleport) {
                message = message.setStyle(
                        message.getStyle().withClickEvent(
                                new ClickEvent.RunCommand("/tp @s %d %d %d".formatted(anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()))));
            }

            player.sendSystemMessage(message);
        }

        player.sendSystemMessage(buildPaginationFooter(page, totalPages, filter, sort, near, radius));

        return pageAnchors.size();
    }

    private static List<Anchor> sortAnchors(List<Anchor> anchors, AnchorSort sort, BlockPos origin) {
        return switch (sort) {
            case NAME -> AnchorListView.sortByGroupThenName(anchors);
            case DISTANCE -> AnchorListView.groupByFirstOccurrence(AnchorListView.sortByDistance(anchors, origin));
            case RECENT -> AnchorListView.groupByFirstOccurrence(AnchorListView.sortByRecent(anchors));
        };
    }

    private static MutableComponent anchorRow(int index, Anchor anchor, BlockPos origin, boolean showDistance) {
        String base = "  [%d] %s [%d, %d, %d]"
                .formatted(index, displayName(anchor), anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ());
        if (!showDistance) {
            return Component.literal(base);
        }
        return Component.literal("%s - %dm".formatted(base, Math.round(Math.sqrt(anchor.pos.distSqr(origin)))));
    }

    private static MutableComponent buildPaginationFooter(
            int page,
            int totalPages,
            String filter,
            AnchorSort sort,
            boolean near,
            int radius) {
        boolean hasPrev = page > 1;
        boolean hasNext = page < totalPages;

        MutableComponent prev = Component.literal("[« Prev]")
                .setStyle(Style.EMPTY.withColor(hasPrev ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (hasPrev) {
            prev = prev.setStyle(prev.getStyle().withClickEvent(
                    new ClickEvent.RunCommand(rerunCommand(page - 1, filter, sort, near, radius))));
        }

        MutableComponent next = Component.literal("[Next »]")
                .setStyle(Style.EMPTY.withColor(hasNext ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (hasNext) {
            next = next.setStyle(next.getStyle().withClickEvent(
                    new ClickEvent.RunCommand(rerunCommand(page + 1, filter, sort, near, radius))));
        }

        return Component.empty()
                .append(prev)
                .append(Component.literal(" page %d/%d ".formatted(page, totalPages)))
                .append(next);
    }

    private static String rerunCommand(int page, String filter, AnchorSort sort, boolean near, int radius) {
        if (near) {
            return "/sp anchor near %d %d".formatted(radius, page);
        }
        if (filter == null || filter.isEmpty()) {
            if (sort == AnchorSort.NAME) {
                return "/sp anchor list %d".formatted(page);
            }
            return "/sp anchor list %d %s".formatted(page, sort.flag());
        }
        if (sort == AnchorSort.NAME) {
            return "/sp anchor list %s %d".formatted(filter, page);
        }
        return "/sp anchor list %s %d %s".formatted(filter, page, sort.flag());
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> sortLiteral(
            String flagLiteral,
            AnchorSort sort,
            String filter,
            int page) {
        return literal(flagLiteral).executes(context -> AnchorCommand.listAnchors(context.getSource(), filter, page, sort));
    }

    private static Map<String, Integer> groupCounts(List<Anchor> anchors) {
        Map<String, Integer> counts = new HashMap<>();
        for (Anchor anchor : anchors) {
            counts.merge(normalizeGroup(anchor.group), 1, Integer::sum);
        }
        return counts;
    }

    private static String normalizeGroup(String group) {
        return AnchorCreation.normalizeGroup(group);
    }

    private static String groupLabel(String group) {
        return group.isEmpty() ? "(ungrouped)" : group;
    }

    private static String displayName(Anchor anchor) {
        String group = normalizeGroup(anchor.group);
        if (group.isEmpty()) return anchor.name;
        return "%s/%s".formatted(group, anchor.name);
    }
}
