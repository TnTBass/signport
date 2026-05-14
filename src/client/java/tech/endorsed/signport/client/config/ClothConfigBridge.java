package tech.endorsed.signport.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClothConfigBridge {
    private ClothConfigBridge() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("SignPort"));
        var entryBuilder = builder.entryBuilder();
        builder.getOrCreateCategory(Component.literal("HUD"))
                .addEntry(entryBuilder.startBooleanToggle(
                                Component.literal("HUD lookup hint"),
                                SignPortClientConfig.get().hudHintEnabled)
                        .setDefaultValue(SignPortClientConfig.Values.defaults().hudHintEnabled)
                        .setSaveConsumer(SignPortClientConfig::setHudHintEnabled)
                        .build());
        builder.setSavingRunnable(SignPortClientConfig::save);
        return builder.build();
    }
}
