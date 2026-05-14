package tech.endorsed.signport.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.client.config.ConfigScreenFactory;
import tech.endorsed.signport.client.config.SignPortClientConfig;
import tech.endorsed.signport.client.gui.AnchorBrowserScreen;
import tech.endorsed.signport.client.hud.PortSignHudHint;
import tech.endorsed.signport.network.AnchorSyncPayloads;

public class SignPortClient implements ClientModInitializer {
    private static final Category SIGNPORT_CATEGORY =
            Category.register(Identifier.fromNamespaceAndPath(SignPort.MOD_ID, "signport"));
    private static KeyMapping configKey;
    private static KeyMapping browserKey;

    @Override
    public void onInitializeClient() {
        SignPortClientConfig.load();
        AnchorSyncPayloads.registerClientbound();

        ClientPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.FULL_TYPE, (payload, context) ->
                context.client().execute(() -> SignPortClientState.applyFull(payload)));
        ClientPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.DELTA_TYPE, (payload, context) ->
                context.client().execute(() -> SignPortClientState.applyDelta(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SignPortClientState.clear());

        configKey = registerKeyBinding(new KeyMapping(
                "key.signport.config",
                InputConstants.UNKNOWN.getValue(),
                SIGNPORT_CATEGORY));
        if (SignPortClientConfig.get().browserKeybindEnabled) {
            browserKey = registerKeyBinding(new KeyMapping(
                    "key.signport.browser",
                    InputConstants.KEY_J,
                    SIGNPORT_CATEGORY));
        }

        ClientTickEvents.END_CLIENT_TICK.register(SignPortClient::tick);
        SignPort.LOGGER.info("[SignPort] Client foundation initialized.");
    }

    private static KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        try {
            Class<?> helper = Class.forName("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper");
            return (KeyMapping) helper.getMethod("registerKeyBinding", KeyMapping.class).invoke(null, keyMapping);
        } catch (ReflectiveOperationException exception) {
            SignPort.LOGGER.warn("[SignPort] Could not register config keybind; Fabric key binding API is unavailable.", exception);
            return keyMapping;
        }
    }

    private static void tick(Minecraft client) {
        while (configKey.consumeClick()) {
            openConfigScreen(client);
        }
        if (browserKey != null) {
            while (browserKey.consumeClick()) {
                openAnchorBrowser(client);
            }
        }
        PortSignHudHint.tick(client);
    }

    public static void openAnchorBrowser(Minecraft client) {
        if (client.player == null) return;
        client.setScreen(new AnchorBrowserScreen(client.screen));
    }

    public static void openConfigScreen(Minecraft client) {
        if (client.player == null) return;
        if (!ConfigScreenFactory.isAvailable()) {
            client.player.sendSystemMessage(Component.literal(
                    "SignPort: install Cloth Config to use the settings screen; config/signport-client.json can be edited directly."));
            return;
        }
        client.setScreen(ConfigScreenFactory.create(client.screen));
    }
}
