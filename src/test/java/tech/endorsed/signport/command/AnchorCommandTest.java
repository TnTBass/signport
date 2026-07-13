package tech.endorsed.signport.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorCommandTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void teleportCommandAcceptsNamespacedOptionalDimension() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        AnchorCommand.register(dispatcher);

        var name = dispatcher.getRoot()
                .getChild("signport")
                .getChild("tp")
                .getChild("name");
        assertNotNull(name.getCommand(), "the original current-dimension form must remain executable");
        var dimension = name.getChild("dimension");
        assertNotNull(dimension.getCommand(), "the qualified form must be executable");
        assertInstanceOf(IdentifierArgument.class, ((ArgumentCommandNode<?, ?>) dimension).getType(),
                "namespaced dimensions must parse the colon in minecraft:overworld");
    }

    @Test
    void teleportCommandRemainsVisibleWithoutPermissionSoExecutionCanExplainDenial() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        AnchorCommand.register(dispatcher);

        var teleport = dispatcher.getRoot().getChild("signport").getChild("tp");
        assertTrue(teleport.getRequirement().test(null),
                "the teleport branch must not be hidden behind a Brigadier permission requirement");
        assertEquals(
                "Permission denied: you need signport.teleport.command to teleport with SignPort.",
                AnchorCommand.TELEPORT_PERMISSION_DENIED);
    }
}
