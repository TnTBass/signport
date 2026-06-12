package tech.endorsed.signport.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.world.Anchor;

public final class AnchorSyncServer {
    private static Adapter adapter = Adapter.NO_OP;

    private AnchorSyncServer() {
    }

    public static void install(Adapter implementation) {
        adapter = implementation == null ? Adapter.NO_OP : implementation;
    }

    public static void sendFullToAll(MinecraftServer server) {
        adapter.sendFullToAll(server);
    }

    public static void anchorCreated(MinecraftServer server, Anchor anchor) {
        adapter.anchorCreated(server, anchor);
    }

    public static void anchorUpdated(MinecraftServer server, Anchor anchor) {
        adapter.anchorUpdated(server, anchor);
    }

    public static void anchorDeleted(MinecraftServer server, String name, ResourceKey<Level> dimension) {
        adapter.anchorDeleted(server, name, dimension);
    }

    public interface Adapter {
        Adapter NO_OP = new Adapter() {
        };

        default void sendFullToAll(MinecraftServer server) {
        }

        default void anchorCreated(MinecraftServer server, Anchor anchor) {
        }

        default void anchorUpdated(MinecraftServer server, Anchor anchor) {
        }

        default void anchorDeleted(MinecraftServer server, String name, ResourceKey<Level> dimension) {
        }
    }
}
