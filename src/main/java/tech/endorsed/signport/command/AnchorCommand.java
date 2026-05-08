package tech.endorsed.signport.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import oshi.util.tuples.Triplet;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorState;

import java.util.EnumSet;

import static net.minecraft.server.command.CommandManager.literal;

public class AnchorCommand {
    private static final SimpleCommandExceptionType CREATE_FAILED_EXCEPTION
            = new SimpleCommandExceptionType(Text.translatable("commands.anchor.create.failed"));
    private static final SimpleCommandExceptionType NAME_CLASH_EXCEPTION
            = new SimpleCommandExceptionType(Text.translatable("commands.anchor.create.nameclash"));
    private static final SimpleCommandExceptionType UNKNOWN_NAME_EXCEPTION
            = new SimpleCommandExceptionType(Text.translatable("commands.anchor.delete.unknownname"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(
                literal("signport")
                        .then(literal("tp")
                            .requires(SignPortPermissions::canUseTeleportCommand)
                            .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> teleportAnchor(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(literal("anchor")
                                .then(literal("list")
                                        .requires(SignPortPermissions::canListAnchors)
                                        .executes(context -> AnchorCommand.listAnchors(context.getSource())))
                                .then(literal("delete")
                                        .requires(SignPortPermissions::canDeleteAnchor)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(context -> AnchorCommand.deleteAnchor(context.getSource(), StringArgumentType.getString(context, "name")))))
                                .then(literal("create")
                                        .requires(SignPortPermissions::canCreateAnchor)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(context -> AnchorCommand.createAnchor(context.getSource(), StringArgumentType.getString(context, "name"), null))
                                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                        .executes(context -> AnchorCommand.createAnchor(context.getSource(), StringArgumentType.getString(context, "name"), BlockPosArgumentType.getLoadedBlockPos(context, "pos"))))))));

        dispatcher.register(literal("sp")
                .redirect(literalCommandNode));
    }

    private static int teleportAnchor(ServerCommandSource source, String name) {
        var player = source.getPlayer();
        if (player == null) return 0;

        var world = source.getWorld();
        if (world == null) return 0;

        var state = AnchorState.getServerState(world);

        for (Anchor anchor: state.GetAnchors()) {
            if (name.equals(anchor.name)) {
                player.teleport(world,
                        anchor.pos.getX(),
                        anchor.pos.getY(),
                        anchor.pos.getZ(),
                        EnumSet.noneOf(PositionFlag.class),
                        player.getYaw(),
                        player.getPitch(),
                        false);
                return 1;
            }
        }

        player.sendMessage(Text.literal("Could not find anchor '%s'".formatted(name)));
        return 0;
    }

    public static int createAnchor(ServerCommandSource source, String name, BlockPos pos) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getWorld());
        if (anchorState == null) return 0;

        for (Anchor anchor : anchorState.GetAnchors()) {
            if (anchor.name.equals(name)) throw NAME_CLASH_EXCEPTION.create();
            if (anchor.pos.equals(pos)) throw CREATE_FAILED_EXCEPTION.create();
        }

        var aPos = pos == null ? source.getPlayer().getBlockPos() : pos;
        Anchor anchor = new Anchor(name, aPos);
        anchorState.anchors.add(anchor);
        anchorState.markDirty();

        player.sendMessage(Text.literal("Created anchor '%s'".formatted(name)));

        return 1;
    }

    public static int deleteAnchor(ServerCommandSource source, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getWorld());
        if (anchorState == null) return 0;

        int i = 0;
        for (Anchor anchor : anchorState.GetAnchors()) {
            if (anchor.name.equals(name)) {
                anchorState.anchors.remove(i);
                anchorState.markDirty();
                player.sendMessage(Text.literal("Deleted anchor '%s'".formatted(name)));
                return 1;
            }
            i = i + 1;
        }

        if (name.equalsIgnoreCase("all")) {
            anchorState.anchors.clear();
            anchorState.markDirty();
            player.sendMessage(Text.literal("Deleted ALL anchors"));
            return 1;
        }

        throw UNKNOWN_NAME_EXCEPTION.create();
    }

    public static int listAnchors(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getWorld());
        if (anchorState == null) return 0;
        if (anchorState.GetAnchors().isEmpty()) {
            player.sendMessage(Text.literal("No anchors exist"));
            return 1;
        }

        int i = 1;
        for (Anchor anchor : anchorState.GetAnchors()) {
            MutableText message = Text.literal("[%d] %s [%d, %d, %d]"
                    .formatted(i, anchor.name, anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()));

            if (player.hasPermissionLevel(2)) {
                message = message.setStyle(
                        message.getStyle().withClickEvent(
                                new ClickEvent.RunCommand("/tp @s %d %d %d".formatted(anchor.pos.getX(), anchor.pos.getY(), anchor.pos.getZ()))));
            }

            player.sendMessage(message);
            i = i + 1;
        }
        return i - 1;
    }
}
