package tech.endorsed.signport.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
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
import tech.endorsed.signport.network.StatusPayloads;
import tech.endorsed.signport.status.SignPortStatus;

public class SignPortClient implements ClientModInitializer {
    private static final Category SIGNPORT_CATEGORY =
            Category.register(Identifier.fromNamespaceAndPath(SignPort.MOD_ID, "signport"));
    private static KeyMapping configKey;
    private static KeyMapping browserKey;
    private static boolean initialSyncRequested = false;
    private static boolean statusPayloadReceived = false;
    private static boolean statusCleanedUp = true;
    private static int ticksSinceJoin = 0;

    @Override
    public void onInitializeClient() {
        SignPortClientConfig.load();
        AnchorSyncPayloads.registerClientbound();
        AnchorSyncPayloads.registerServerbound();
        StatusPayloads.registerClientbound();

        ClientPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.FULL_TYPE, (payload, context) ->
                context.client().execute(() -> SignPortClientState.applyFull(payload)));
        ClientPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.DELTA_TYPE, (payload, context) ->
                context.client().execute(() -> SignPortClientState.applyDelta(payload)));
        ClientPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().screen instanceof AnchorBrowserScreen browser) {
                        browser.handleCreateAnchorResponse(payload);
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(StatusPayloads.VERSION_TYPE, (payload, context) ->
                context.client().execute(() -> {
                    SignPortStatus.onServerStatus(SignPortStatus.decodeServerStatus(payload.value()));
                    statusPayloadReceived = true;
                }));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SignPortStatus.onClientJoin();
            statusPayloadReceived = false;
            statusCleanedUp = false;
            ticksSinceJoin = 0;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SignPortClientState.clear();
            cleanupStatus();
            initialSyncRequested = false;
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> cleanupStatus());

        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.signport.config",
                InputConstants.UNKNOWN.getValue(),
                SIGNPORT_CATEGORY));
        if (SignPortClientConfig.get().browserKeybindEnabled) {
            browserKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.signport.browser",
                    InputConstants.KEY_J,
                    SIGNPORT_CATEGORY));
        }

        ClientTickEvents.END_CLIENT_TICK.register(SignPortClient::tick);
        SignPort.LOGGER.info("[SignPort] Client initialized.");
    }

    private static void tick(Minecraft client) {
        updateStatusDetection(client);
        requestInitialSyncWhenReady(client);
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

    private static void updateStatusDetection(Minecraft client) {
        if (client.player == null || statusPayloadReceived) return;
        if (SignPortStatus.shouldMarkServerNotDetected(true, false, ticksSinceJoin)) {
            if (SignPortStatus.clientState().markServerNotDetectedIfUnknown()) {
                statusPayloadReceived = true;
            }
        }
        ticksSinceJoin++;
    }

    private static void cleanupStatus() {
        if (statusCleanedUp) return;
        SignPortStatus.onClientDisconnect();
        statusPayloadReceived = false;
        statusCleanedUp = true;
        ticksSinceJoin = 0;
    }

    private static void requestInitialSyncWhenReady(Minecraft client) {
        boolean playerPresent = client.player != null;
        boolean canSendReady = false;
        if (playerPresent) {
            canSendReady = ClientPlayNetworking.canSend(AnchorSyncPayloads.READY_TYPE);
        }
        if (AnchorSyncPayloads.shouldRequestInitialSync(playerPresent, canSendReady, initialSyncRequested)) {
            initialSyncRequested = true;
            if (SignPortClientState.anchors().isEmpty()) {
                ClientPlayNetworking.send(new AnchorSyncPayloads.Ready());
            }
        }
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
