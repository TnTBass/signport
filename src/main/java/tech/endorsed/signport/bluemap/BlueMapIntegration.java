package tech.endorsed.signport.bluemap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.world.Anchor;

import java.lang.reflect.Method;

public final class BlueMapIntegration {
    private static boolean available;
    private static boolean initialized;

    private BlueMapIntegration() {
    }

    public static void initialize() {
        if (!SignPortConfig.get().bluemapEnabled()) {
            SignPort.LOGGER.info("[SignPort] BlueMap integration disabled by config.");
            return;
        }

        if (!FabricLoader.getInstance().isModLoaded("bluemap")) {
            SignPort.LOGGER.info("[SignPort] BlueMap not detected, integration disabled.");
            return;
        }

        if (invoke("initialize", new Class<?>[] {})) {
            available = true;
            initialized = true;
            SignPort.LOGGER.info("[SignPort] BlueMap detected, anchor marker integration enabled.");
        }
    }

    public static void serverStarted(MinecraftServer server) {
        if (!active()) return;
        invoke("serverStarted", new Class<?>[] { MinecraftServer.class }, server);
    }

    public static void serverStopping(MinecraftServer server) {
        if (!active()) return;
        invoke("serverStopping", new Class<?>[] { MinecraftServer.class }, server);
    }

    public static void anchorCreated(MinecraftServer server, Anchor anchor) {
        if (!active()) return;
        invoke("upsertAnchor", new Class<?>[] { MinecraftServer.class, Anchor.class }, server, anchor);
    }

    public static void anchorUpdated(MinecraftServer server, Anchor anchor) {
        if (!active()) return;
        invoke("upsertAnchor", new Class<?>[] { MinecraftServer.class, Anchor.class }, server, anchor);
    }

    public static void anchorDeleted(MinecraftServer server, ResourceKey<Level> dimension, String name) {
        if (!active()) return;
        invoke("removeAnchor", new Class<?>[] { MinecraftServer.class, ResourceKey.class, String.class }, server, dimension, name);
    }

    public static void anchorsCleared(MinecraftServer server, ResourceKey<Level> dimension) {
        if (!active()) return;
        invoke("clearDimension", new Class<?>[] { MinecraftServer.class, ResourceKey.class }, server, dimension);
    }

    private static boolean active() {
        return available && initialized && SignPortConfig.get().bluemapEnabled();
    }

    private static boolean invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> bridge = Class.forName("tech.endorsed.signport.bluemap.BlueMapBridge");
            Method method = bridge.getMethod(methodName, parameterTypes);
            method.invoke(null, args);
            return true;
        } catch (ReflectiveOperationException exception) {
            available = false;
            SignPort.LOGGER.warn("[SignPort] BlueMap integration unavailable; anchor markers disabled.", exception);
            return false;
        }
    }
}
