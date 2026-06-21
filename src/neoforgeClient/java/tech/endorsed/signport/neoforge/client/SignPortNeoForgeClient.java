package tech.endorsed.signport.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.client.ScreenNavigation;
import tech.endorsed.signport.client.SignPortClientState;
import tech.endorsed.signport.client.config.ConfigScreenFactory;
import tech.endorsed.signport.client.config.SignPortClientConfig;
import tech.endorsed.signport.client.gui.AnchorBrowserScreen;
import tech.endorsed.signport.client.hud.PortSignHudHint;
import tech.endorsed.signport.network.AnchorSyncPayloads;
import tech.endorsed.signport.network.SignPortStatusNetworking;
import tech.endorsed.signport.network.StatusPayloads;
import tech.endorsed.signport.status.SignPortStatus;

@Mod(value = SignPort.MOD_ID, dist = Dist.CLIENT)
public final class SignPortNeoForgeClient {
    private static final Category SIGNPORT_CATEGORY =
            Category.register(Identifier.fromNamespaceAndPath(SignPort.MOD_ID, "signport"));
    private static final KeyMapping configKey = new KeyMapping(
            "key.signport.config",
            InputConstants.UNKNOWN.getValue(),
            SIGNPORT_CATEGORY);
    private static final KeyMapping browserKey = new KeyMapping(
            "key.signport.browser",
            InputConstants.KEY_J,
            SIGNPORT_CATEGORY);
    private static boolean initialSyncRequested = false;
    private static boolean statusPayloadReceived = false;
    private static int lastStatusRequestTick = SignPortStatusNetworking.NO_SERVER_VERSION_REQUEST_TICK;
    private static boolean statusCleanedUp = true;
    private static int ticksSinceJoin = 0;

    public SignPortNeoForgeClient(IEventBus modBus) {
        SignPortClientConfig.load(FMLPaths.CONFIGDIR.get().resolve(SignPortClientConfig.FILE_NAME));
        ScreenNavigation.install(screen -> Minecraft.getInstance().setScreenAndShow(screen));
        SignPortStatus.installVersionSupplier(SignPortNeoForgeClient::resolveVersion);
        AnchorBrowserScreen.installCreateAnchorSender(ClientPacketDistributor::sendToServer);

        modBus.addListener(SignPortNeoForgeClient::registerClientPayloadHandlers);
        modBus.addListener(SignPortNeoForgeClient::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(SignPortNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SignPortNeoForgeClient::onClientLoggingIn);
        NeoForge.EVENT_BUS.addListener(SignPortNeoForgeClient::onClientLoggingOut);
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (container, parent) ->
                ConfigScreenFactory.isAvailable() ? ConfigScreenFactory.create(parent) : parent);
        SignPort.LOGGER.info("[SignPort] NeoForge client initialized.");
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(configKey);
        event.register(browserKey);
    }

    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(AnchorSyncPayloads.FULL_TYPE, (payload, context) ->
                context.enqueueWork(() -> SignPortClientState.applyFull(payload)));
        event.register(AnchorSyncPayloads.DELTA_TYPE, (payload, context) ->
                context.enqueueWork(() -> SignPortClientState.applyDelta(payload)));
        event.register(AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE, (payload, context) ->
                context.enqueueWork(() -> {
                    if (AnchorBrowserScreen.active() instanceof AnchorBrowserScreen browser) {
                        browser.handleCreateAnchorResponse(payload);
                    }
                }));
        event.register(StatusPayloads.VERSION_TYPE, (payload, context) ->
                context.enqueueWork(() -> {
                    SignPortStatus.onServerStatus(SignPortStatus.decodeServerStatus(payload.value()));
                    statusPayloadReceived = true;
                }));
    }

    private static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        SignPortClientState.clear();
        SignPortStatus.onClientJoin();
        initialSyncRequested = false;
        statusPayloadReceived = false;
        lastStatusRequestTick = SignPortStatusNetworking.NO_SERVER_VERSION_REQUEST_TICK;
        statusCleanedUp = false;
        ticksSinceJoin = 0;
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SignPortClientState.clear();
        cleanupStatus();
        initialSyncRequested = false;
        lastStatusRequestTick = SignPortStatusNetworking.NO_SERVER_VERSION_REQUEST_TICK;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        requestServerVersionWhenReady(client);
        updateStatusDetection(client);
        requestInitialSyncWhenReady(client);
        while (configKey.consumeClick()) {
            openConfigScreen(client);
        }
        while (browserKey.consumeClick()) {
            if (SignPortClientConfig.get().browserKeybindEnabled) {
                openAnchorBrowser(client);
            }
        }
        PortSignHudHint.tick(client);
    }

    private static void updateStatusDetection(Minecraft client) {
        if (client.player == null || statusPayloadReceived) return;
        if (SignPortStatus.shouldMarkServerNotDetected(true, false, ticksSinceJoin)) {
            SignPortStatus.clientState().markServerNotDetectedIfUnknown();
        }
        ticksSinceJoin++;
    }

    private static void cleanupStatus() {
        if (statusCleanedUp) return;
        SignPortStatus.onClientDisconnect();
        statusPayloadReceived = false;
        lastStatusRequestTick = SignPortStatusNetworking.NO_SERVER_VERSION_REQUEST_TICK;
        statusCleanedUp = true;
        ticksSinceJoin = 0;
    }

    private static void requestServerVersionWhenReady(Minecraft client) {
        boolean playerPresent = client.player != null;
        ClientPacketListener listener = client.getConnection();
        boolean canSendRequest = playerPresent
                && listener instanceof ICommonPacketListener
                && ((ICommonPacketListener) listener).hasChannel(StatusPayloads.REQUEST_ID);
        if (SignPortStatusNetworking.shouldRequestServerVersion(
                playerPresent,
                canSendRequest,
                statusPayloadReceived,
                ticksSinceJoin,
                lastStatusRequestTick
        )) {
            ClientPacketDistributor.sendToServer(StatusPayloads.ServerVersionRequest.INSTANCE);
            lastStatusRequestTick = ticksSinceJoin;
        }
    }

    private static void requestInitialSyncWhenReady(Minecraft client) {
        boolean playerPresent = client.player != null;
        ClientPacketListener listener = client.getConnection();
        boolean canSendReady = playerPresent
                && listener instanceof ICommonPacketListener
                && ((ICommonPacketListener) listener).hasChannel(AnchorSyncPayloads.READY_ID);
        if (AnchorSyncPayloads.shouldRequestInitialSync(playerPresent, canSendReady, initialSyncRequested)) {
            initialSyncRequested = true;
            if (SignPortClientState.anchors().isEmpty()) {
                ClientPacketDistributor.sendToServer(new AnchorSyncPayloads.Ready());
            }
        }
    }

    public static void openAnchorBrowser(Minecraft client) {
        if (client.player == null) return;
        ScreenNavigation.show(new AnchorBrowserScreen(null));
    }

    public static void openConfigScreen(Minecraft client) {
        if (client.player == null) return;
        ScreenNavigation.show(ConfigScreenFactory.create(null));
    }

    private static String resolveVersion() {
        return ModList.get()
                .getModContainerById(SignPort.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("Unknown");
    }
}
