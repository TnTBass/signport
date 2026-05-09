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
import net.minecraft.util.math.Vec3d;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorState;
import tech.endorsed.signport.world.TeleportDestinationResolver;

import java.util.EnumSet;
import java.util.Optional;

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

        Optional<Anchor> anchor = state.findAnchor(name);
        if (anchor.isPresent()) {
            Optional<Vec3d> destination = TeleportDestinationResolver.resolve(world, anchor.get().pos);
            if (destination.isEmpty()) {
                player.sendMessage(Text.literal("Could not find a safe destination near anchor '%s'".formatted(anchor.get().name)));
                return 0;
            }

            Vec3d pos = destination.get();
            player.teleport(world,
                    pos.x,
                    pos.y,
                    pos.z,
                    EnumSet.noneOf(PositionFlag.class),
                    player.getYaw(),
                    player.getPitch(),
                    false);
            return 1;
        }

        player.sendMessage(Text.literal("Could not find anchor '%s'".formatted(name)));
        return 0;
    }

    public static int createAnchor(ServerCommandSource source, String name, BlockPos pos) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getWorld());
        if (anchorState == null) return 0;

        var aPos = pos == null ? source.getPlayer().getBlockPos() : pos;
        if (anchorState.findAnchor(name).isPresent()) throw NAME_CLASH_EXCEPTION.create();
        for (Anchor anchor : anchorState.GetAnchors()) {
            if (anchor.pos.equals(aPos)) throw CREATE_FAILED_EXCEPTION.create();
        }

        Anchor anchor = new Anchor(name, aPos);
        anchorState.addAnchor(anchor);

        player.sendMessage(Text.literal("Created anchor '%s'".formatted(name)));

        return 1;
    }

    public static int deleteAnchor(ServerCommandSource source, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        AnchorState anchorState = AnchorState.getServerState(source.getWorld());
        if (anchorState == null) return 0;

        if (anchorState.deleteAnchor(name)) {
            player.sendMessage(Text.literal("Deleted anchor '%s'".formatted(name)));
            return 1;
        }

        if (name.equalsIgnoreCase("all")) {
            anchorState.clearAnchors();
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

            if (player.hasPermissionLevel(SignPortConfig.get().protectedActionOpLevel())) {
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
