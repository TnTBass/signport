package tech.endorsed.signport.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorState;
import tech.endorsed.signport.world.TeleportDestinationResolver;

import java.util.EnumSet;
import java.util.Optional;

import static net.minecraft.commands.Commands.literal;

public class AnchorCommand {
    private static final SimpleCommandExceptionType CREATE_FAILED_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.create.failed"));
    private static final SimpleCommandExceptionType NAME_CLASH_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.create.nameclash"));
    private static final SimpleCommandExceptionType UNKNOWN_NAME_EXCEPTION
            = new SimpleCommandExceptionType(Component.translatable("commands.anchor.delete.unknownname"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(
                literal("signport")
                        .then(literal("tp")
                            .requires(SignPortPermissions::canUseTeleportCommand)
                            .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> teleportAnchor(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(literal("anchor")
                                .then(literal("list")
                                        .requires(SignPortPermissions::canListAnchors)
                                        .executes(context -> AnchorCommand.listAnchors(context.getSource())))
                                .then(literal("delete")
                                        .requires(SignPortPermissions::canDeleteAnchor)
                                        .then(Commands.argument("name", StringArgumentType.word())
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

        var state = AnchorState.getServerState(world);

        Optional<Anchor> anchor = state.findAnchor(name);
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

        AnchorState anchorState = AnchorState.getServerState(source.getLevel());
        if (anchorState == null) return 0;

        var aPos = pos == null ? source.getPlayer().blockPosition() : pos;
        if (anchorState.findAnchor(name).isPresent()) throw NAME_CLASH_EXCEPTION.create();
        for (Anchor anchor : anchorState.GetAnchors()) {
            if (anchor.pos.equals(aPos)) throw CREATE_FAILED_EXCEPTION.create();
        }

        Anchor anchor = new Anchor(name, aPos);
        anchorState.addAnchor(anchor);

        player.sendSystemMessage(Component.literal("Created anchor '%s'".formatted(name)));

        return 1;
    }

    public static int deleteAnchor(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getLevel());
        if (anchorState == null) return 0;

        if (anchorState.deleteAnchor(name)) {
            player.sendSystemMessage(Component.literal("Deleted anchor '%s'".formatted(name)));
            return 1;
        }

        if (name.equalsIgnoreCase("all")) {
            anchorState.clearAnchors();
            player.sendSystemMessage(Component.literal("Deleted ALL anchors"));
            return 1;
        }

        throw UNKNOWN_NAME_EXCEPTION.create();
    }

    public static int listAnchors(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getLevel());
        if (anchorState == null) return 0;
        if (anchorState.GetAnchors().isEmpty()) {
            player.sendSystemMessage(Component.literal("No anchors exist"));
            return 1;
        }

        int i = 1;
        for (Anchor anchor : anchorState.GetAnchors()) {
            MutableComponent message = Component.literal("[%d] %s [%d, %d, %d]"
                    .formatted(i, anchor.name, anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()));

            if (player.permissions() instanceof LevelBasedPermissionSet pls &&
                    pls.level().isEqualOrHigherThan(PermissionLevel.byId(SignPortConfig.get().protectedActionOpLevel()))) {
                message = message.setStyle(
                        message.getStyle().withClickEvent(
                                new ClickEvent.RunCommand("/tp @s %d %d %d".formatted(anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()))));
            }

            player.sendSystemMessage(message);
            i = i + 1;
        }
        return i - 1;
    }
}
