package tech.endorsed.signport.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tech.endorsed.signport.internal.modstatus.ModStatusDisplay;
import tech.endorsed.signport.status.SignPortStatus;

public final class ClothConfigBridge {
    private ClothConfigBridge() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("SignPort"));
        var entryBuilder = builder.entryBuilder();
        ModStatusDisplay status = SignPortStatus.clientState().display();
        // Status rows are informational and reflect the status known when the screen opens.
        builder.getOrCreateCategory(Component.literal("Status"))
                .addEntry(entryBuilder.startTextDescription(Component.literal(
                        status.statusLabel() + ": " + status.helpText())).build())
                .addEntry(entryBuilder.startTextDescription(Component.literal(
                        "Client: " + SignPortStatus.versionWithBuild(status.clientVersion(), status.clientBuild()))).build())
                .addEntry(entryBuilder.startTextDescription(Component.literal(
                        "Server: " + SignPortStatus.versionWithBuild(status.serverVersion(), status.serverBuild()))).build());
        builder.getOrCreateCategory(Component.literal("HUD"))
                .addEntry(entryBuilder.startBooleanToggle(
                                Component.literal("HUD lookup hint"),
                                SignPortClientConfig.get().hudHintEnabled)
                        .setDefaultValue(SignPortClientConfig.Values.defaults().hudHintEnabled)
                        .setSaveConsumer(SignPortClientConfig::setHudHintEnabled)
                        .build());
        builder.getOrCreateCategory(Component.literal("Browser"))
                .addEntry(entryBuilder.startBooleanToggle(
                                Component.literal("Anchor browser keybind (restart required)"),
                                SignPortClientConfig.get().browserKeybindEnabled)
                        .setDefaultValue(SignPortClientConfig.Values.defaults().browserKeybindEnabled)
                        .setSaveConsumer(SignPortClientConfig::setBrowserKeybindEnabled)
                        .build())
                .addEntry(entryBuilder.startBooleanToggle(
                                Component.literal("Sign editor autocomplete"),
                                SignPortClientConfig.get().signEditorAutocompleteEnabled)
                        .setDefaultValue(SignPortClientConfig.Values.defaults().signEditorAutocompleteEnabled)
                        .setSaveConsumer(SignPortClientConfig::setSignEditorAutocompleteEnabled)
                        .build())
                .addEntry(entryBuilder.startBooleanToggle(
                                Component.literal("Sign template button"),
                                SignPortClientConfig.get().signTemplateButtonEnabled)
                        .setDefaultValue(SignPortClientConfig.Values.defaults().signTemplateButtonEnabled)
                        .setSaveConsumer(SignPortClientConfig::setSignTemplateButtonEnabled)
                        .build());
        builder.setSavingRunnable(SignPortClientConfig::save);
        return builder.build();
    }
}
