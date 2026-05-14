package tech.endorsed.signport.client.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("cloth-config");
    }

    public static Screen create(Screen parent) {
        if (!isAvailable()) return parent;
        return ClothConfigBridge.create(parent);
    }
}
