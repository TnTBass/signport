package tech.endorsed.signport.fabric.bluemap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.world.Anchor;

import java.lang.reflect.Method;

public final class FabricBlueMapIntegration implements BlueMapIntegration.Adapter {
    private static boolean available;
    private static boolean initialized;

    public FabricBlueMapIntegration() {
    }

    @Override
    public void initialize() {
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

    @Override
    public void serverStarted(MinecraftServer server) {
        if (!active()) return;
        invoke("serverStarted", new Class<?>[] { MinecraftServer.class }, server);
    }

    @Override
    public void serverStopping(MinecraftServer server) {
        if (!active()) return;
        invoke("serverStopping", new Class<?>[] { MinecraftServer.class }, server);
    }

    @Override
    public void anchorCreated(MinecraftServer server, Anchor anchor) {
        if (!active()) return;
        invoke("upsertAnchor", new Class<?>[] { MinecraftServer.class, Anchor.class }, server, anchor);
    }

    @Override
    public void anchorUpdated(MinecraftServer server, Anchor anchor) {
        if (!active()) return;
        invoke("upsertAnchor", new Class<?>[] { MinecraftServer.class, Anchor.class }, server, anchor);
    }

    @Override
    public void anchorDeleted(MinecraftServer server, ResourceKey<Level> dimension, String name) {
        if (!active()) return;
        invoke("removeAnchor", new Class<?>[] { MinecraftServer.class, ResourceKey.class, String.class }, server, dimension, name);
    }

    @Override
    public void anchorsCleared(MinecraftServer server, ResourceKey<Level> dimension) {
        if (!active()) return;
        invoke("clearDimension", new Class<?>[] { MinecraftServer.class, ResourceKey.class }, server, dimension);
    }

    private static boolean active() {
        return available && initialized && SignPortConfig.get().bluemapEnabled();
    }

    private static boolean invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> bridge = Class.forName("tech.endorsed.signport.fabric.bluemap.FabricBlueMapBridge");
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
