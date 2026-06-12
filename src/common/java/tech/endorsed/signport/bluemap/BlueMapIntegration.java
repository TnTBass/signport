package tech.endorsed.signport.bluemap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.world.Anchor;

public final class BlueMapIntegration {
    private static Adapter adapter = Adapter.NO_OP;

    private BlueMapIntegration() {
    }

    public static void install(Adapter implementation) {
        adapter = implementation == null ? Adapter.NO_OP : implementation;
    }

    public static void initialize() {
        adapter.initialize();
    }

    public static void serverStarted(MinecraftServer server) {
        adapter.serverStarted(server);
    }

    public static void serverStopping(MinecraftServer server) {
        adapter.serverStopping(server);
    }

    public static void anchorCreated(MinecraftServer server, Anchor anchor) {
        adapter.anchorCreated(server, anchor);
    }

    public static void anchorUpdated(MinecraftServer server, Anchor anchor) {
        adapter.anchorUpdated(server, anchor);
    }

    public static void anchorDeleted(MinecraftServer server, ResourceKey<Level> dimension, String name) {
        adapter.anchorDeleted(server, dimension, name);
    }

    public static void anchorsCleared(MinecraftServer server, ResourceKey<Level> dimension) {
        adapter.anchorsCleared(server, dimension);
    }

    public interface Adapter {
        Adapter NO_OP = new Adapter() {
        };

        default void initialize() {
        }

        default void serverStarted(MinecraftServer server) {
        }

        default void serverStopping(MinecraftServer server) {
        }

        default void anchorCreated(MinecraftServer server, Anchor anchor) {
        }

        default void anchorUpdated(MinecraftServer server, Anchor anchor) {
        }

        default void anchorDeleted(MinecraftServer server, ResourceKey<Level> dimension, String name) {
        }

        default void anchorsCleared(MinecraftServer server, ResourceKey<Level> dimension) {
        }
    }
}
