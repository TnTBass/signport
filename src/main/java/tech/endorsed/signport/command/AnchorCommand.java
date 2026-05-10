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
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorState;
import tech.endorsed.signport.world.TeleportDestinationResolver;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.literal;

public class AnchorCommand {
    private static final SimpleCommandExceptionType CREATE_FAILED_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.create.failed"));
    private static final SimpleCommandExceptionType NAME_CLASH_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.create.nameclash"));
    private static final SimpleCommandExceptionType UNKNOWN_NAME_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.delete.unknownname"));

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
                                        .executes(context -> AnchorCommand.listAnchors(context.getSource(), "", 1))
                                        // /sp anchor list <page> — numeric arg is treated as page
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(context -> AnchorCommand.listAnchors(
                                                        context.getSource(), "", IntegerArgumentType.getInteger(context, "page"))))
                                        // /sp anchor list <filter> [page] — non-numeric arg is treated as substring filter
                                        .then(Commands.argument("filter", StringArgumentType.word())
                                                .executes(context -> AnchorCommand.listAnchors(
                                                        context.getSource(), StringArgumentType.getString(context, "filter"), 1))
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(context -> AnchorCommand.listAnchors(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "filter"),
                                                                IntegerArgumentType.getInteger(context, "page"))))))
                                .then(literal("delete")
                                        .requires(SignPortPermissions::canDeleteAnchor)
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .suggests(ANCHOR_NAME_SUGGESTIONS)
                                                .executes(context -> AnchorCommand.deleteAnchor(context.getSource(), StringArgumentType.getString(context, "name")))))
                                .then(literal("create")
                                        .requires(SignPortPermissions::canCreateAnchor)
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(context -> AnchorCommand.createAnchor(context.getSource(), StringArgumentType.getString(context, "name"), null))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(context -> AnchorCommand.createAnchor(context.getSource(), StringArgumentType.getString(context, "name"), BlockPosArgument.getLoadedBlockPos(context, "pos"))))))));

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

    public static int createAnchor(CommandSourceStack source, String name, BlockPos pos) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        AnchorState anchorState = AnchorState.getServerState(source.getServer());

        var aPos = pos == null ? source.getPlayer().blockPosition() : pos;
        if (anchorState.findAnchor(name, dim).isPresent()) throw NAME_CLASH_EXCEPTION.create();
        for (Anchor anchor : anchorState.getAnchorsForDimension(dim)) {
            if (anchor.pos.equals(aPos)) throw CREATE_FAILED_EXCEPTION.create();
        }

        Anchor anchor = new Anchor(name, aPos, dim);
        anchorState.addAnchor(anchor);

        player.sendSystemMessage(Component.literal("Created anchor '%s'".formatted(name)));

        return 1;
    }

    public static int deleteAnchor(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        AnchorState anchorState = AnchorState.getServerState(source.getServer());

        if (anchorState.deleteAnchor(name, dim)) {
            player.sendSystemMessage(Component.literal("Deleted anchor '%s'".formatted(name)));
            return 1;
        }

        if (name.equalsIgnoreCase("all")) {
            anchorState.clearAnchors(dim);
            player.sendSystemMessage(Component.literal("Deleted ALL anchors"));
            return 1;
        }

        throw UNKNOWN_NAME_EXCEPTION.create();
    }

    public static int listAnchors(CommandSourceStack source, String filter, int requestedPage) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        var dim = source.getLevel().dimension();
        List<Anchor> dimAnchors = AnchorState.peekServerState(source.getServer())
                .map(s -> s.getAnchorsForDimension(dim))
                .orElse(List.of());
        List<Anchor> matched = AnchorListView.filter(dimAnchors, filter);

        if (matched.isEmpty()) {
            if (filter == null || filter.isEmpty()) {
                player.sendSystemMessage(Component.literal("No anchors exist"));
            } else {
                player.sendSystemMessage(Component.literal("No anchors match '%s'".formatted(filter)));
            }
            return 1;
        }

        int pageSize = SignPortConfig.get().anchorListPageSize();
        int totalPages = AnchorListView.totalPages(matched.size(), pageSize);
        int page = AnchorListView.clampPage(requestedPage, totalPages);
        List<Anchor> pageAnchors = AnchorListView.slice(matched, page, pageSize);

        boolean canTeleport = player.permissions() instanceof LevelBasedPermissionSet pls
                && pls.level().isEqualOrHigherThan(PermissionLevel.byId(SignPortConfig.get().protectedActionOpLevel()));

        int startIndex = (page - 1) * pageSize + 1;
        for (int i = 0; i < pageAnchors.size(); i++) {
            Anchor anchor = pageAnchors.get(i);
            MutableComponent message = Component.literal("[%d] %s [%d, %d, %d]"
                    .formatted(startIndex + i, anchor.name, anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()));

            if (canTeleport) {
                message = message.setStyle(
                        message.getStyle().withClickEvent(
                                new ClickEvent.RunCommand("/tp @s %d %d %d".formatted(anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()))));
            }

            player.sendSystemMessage(message);
        }

        player.sendSystemMessage(buildPaginationFooter(page, totalPages, filter));

        return pageAnchors.size();
    }

    private static MutableComponent buildPaginationFooter(int page, int totalPages, String filter) {
        boolean hasPrev = page > 1;
        boolean hasNext = page < totalPages;

        MutableComponent prev = Component.literal("[« Prev]")
                .setStyle(Style.EMPTY.withColor(hasPrev ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (hasPrev) {
            prev = prev.setStyle(prev.getStyle().withClickEvent(
                    new ClickEvent.RunCommand(rerunCommand(page - 1, filter))));
        }

        MutableComponent next = Component.literal("[Next »]")
                .setStyle(Style.EMPTY.withColor(hasNext ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (hasNext) {
            next = next.setStyle(next.getStyle().withClickEvent(
                    new ClickEvent.RunCommand(rerunCommand(page + 1, filter))));
        }

        return Component.empty()
                .append(prev)
                .append(Component.literal(" page %d/%d ".formatted(page, totalPages)))
                .append(next);
    }

    private static String rerunCommand(int page, String filter) {
        if (filter == null || filter.isEmpty()) {
            return "/sp anchor list %d".formatted(page);
        }
        return "/sp anchor list %s %d".formatted(filter, page);
    }
}
