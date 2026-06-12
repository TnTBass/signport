package tech.endorsed.signport.fabric.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.world.Anchor;
import tech.endorsed.signport.world.AnchorState;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class FabricBlueMapBridge {
    private static final String MARKER_SET_ID = "signport-anchors";
    private static final String MARKER_SET_LABEL = "SignPort Anchors";
    private static final Consumer<BlueMapAPI> ENABLE_LISTENER = FabricBlueMapBridge::onEnable;
    private static final Consumer<BlueMapAPI> DISABLE_LISTENER = FabricBlueMapBridge::onDisable;

    private static MinecraftServer server;
    private static BlueMapAPI api;
    private static boolean listenersRegistered;

    private FabricBlueMapBridge() {
    }

    public static void initialize() {
        if (listenersRegistered) return;
        BlueMapAPI.onEnable(ENABLE_LISTENER);
        BlueMapAPI.onDisable(DISABLE_LISTENER);
        listenersRegistered = true;
    }

    public static void serverStarted(MinecraftServer server) {
        FabricBlueMapBridge.server = server;
        reconcileAll();
    }

    public static void serverStopping(MinecraftServer server) {
        if (FabricBlueMapBridge.server == server) {
            FabricBlueMapBridge.server = null;
        }
    }

    public static void upsertAnchor(MinecraftServer server, Anchor anchor) {
        if (!ready(server)) return;
        server.execute(() -> upsertAnchorOnServerThread(anchor));
    }

    public static void removeAnchor(MinecraftServer server, ResourceKey<Level> dimension, String name) {
        if (!ready(server)) return;
        server.execute(() -> forEachMap(server, dimension, map -> markerSet(map).remove(markerId(name))));
    }

    public static void clearDimension(MinecraftServer server, ResourceKey<Level> dimension) {
        if (!ready(server)) return;
        server.execute(() -> forEachMap(server, dimension, map -> markerSet(map).getMarkers().clear()));
    }

    private static void onEnable(BlueMapAPI api) {
        FabricBlueMapBridge.api = api;
        reconcileAll();
    }

    private static void onDisable(BlueMapAPI ignored) {
        api = null;
    }

    private static void reconcileAll() {
        MinecraftServer currentServer = server;
        if (!ready(currentServer)) return;

        currentServer.execute(() -> {
            if (!SignPortConfig.get().bluemapEnabled()) return;

            for (BlueMapMap map : api.getMaps()) {
                map.getMarkerSets().remove(MARKER_SET_ID);
            }

            List<Anchor> anchors = AnchorState.peekServerState(currentServer)
                    .map(state -> List.copyOf(state.anchors))
                    .orElse(List.of());
            for (Anchor anchor : anchors) {
                upsertAnchorOnServerThread(anchor);
            }
        });
    }

    private static void upsertAnchorOnServerThread(Anchor anchor) {
        MinecraftServer currentServer = server;
        if (!ready(currentServer) || !SignPortConfig.get().bluemapEnabled()) return;

        forEachMap(currentServer, anchor.dimension, map ->
                markerSet(map).put(markerId(anchor.name), marker(anchor)));
    }

    private static void forEachMap(MinecraftServer server, ResourceKey<Level> dimension, Consumer<BlueMapMap> consumer) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null || api == null) return;

        Optional<BlueMapWorld> world = api.getWorld(level);
        if (world.isEmpty()) {
            world = api.getWorld(dimension);
        }

        world.ifPresent(blueMapWorld -> blueMapWorld.getMaps().forEach(consumer));
    }

    private static MarkerSet markerSet(BlueMapMap map) {
        return map.getMarkerSets().computeIfAbsent(MARKER_SET_ID, ignored -> MarkerSet.builder()
                .label(MARKER_SET_LABEL)
                .toggleable(true)
                .defaultHidden(false)
                .sorting(0)
                .build());
    }

    private static POIMarker marker(Anchor anchor) {
        return POIMarker.builder()
                .label(anchor.name)
                .position(anchor.pos.getX() + 0.5D, anchor.pos.getY(), anchor.pos.getZ() + 0.5D)
                .detail(detail(anchor))
                .build();
    }

    private static String detail(Anchor anchor) {
        String group = anchor.group == null || anchor.group.isBlank() ? "ungrouped" : anchor.group;
        String dimension = anchor.dimension.identifier().toString();
        String command = "/sp tp " + anchor.name;
        return """
                <div><b>Group:</b> %s</div>
                <div><b>Dimension:</b> %s</div>
                <div><b>Teleport:</b> <code>%s</code></div>
                """.formatted(html(group), html(dimension), html(command));
    }

    private static String markerId(String name) {
        return "anchor-" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean ready(MinecraftServer server) {
        return server != null && api != null;
    }

    private static String html(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
