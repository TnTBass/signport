package tech.endorsed.signport.client.config;

import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static boolean isAvailable() {
        return true;
    }

    public static Screen create(Screen parent) {
        return new SignPortConfigScreen(parent);
    }
}
