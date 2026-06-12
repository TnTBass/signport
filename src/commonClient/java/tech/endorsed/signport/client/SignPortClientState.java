package tech.endorsed.signport.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.network.AnchorSyncPayloads;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SignPortClientState {
    private static final Map<ResourceKey<Level>, Map<String, AnchorClient>> CACHE = new HashMap<>();
    private static AnchorSyncPayloads.PermissionSnapshot permissions = AnchorSyncPayloads.PermissionSnapshot.empty();
    private static boolean serverHasSignPort = false;

    private SignPortClientState() {
    }

    public static void applyFull(AnchorSyncPayloads.Full payload) {
        CACHE.clear();
        for (AnchorSyncPayloads.SyncedAnchor anchor : payload.anchors()) {
            upsert(AnchorClient.from(anchor));
        }
        permissions = payload.permissions();
        serverHasSignPort = true;
        SignPort.LOGGER.info("[SignPort] Received full anchor sync: {} anchor(s) across {} dimension(s).",
                payload.anchors().size(), CACHE.size());
    }

    public static void applyDelta(AnchorSyncPayloads.Delta payload) {
        if (!serverHasSignPort) return;

        if (payload.action() == AnchorSyncPayloads.Action.DELETE) {
            Map<String, AnchorClient> anchors = CACHE.get(payload.dimension());
            if (anchors != null) {
                anchors.remove(key(payload.name()));
            }
            SignPort.LOGGER.info("[SignPort] Received delta: delete '{}' in {}.",
                    payload.name(), payload.dimension().identifier());
            return;
        }

        upsert(AnchorClient.from(payload.anchor()));
        SignPort.LOGGER.info("[SignPort] Received delta: {} '{}' in {}.",
                payload.action().name().toLowerCase(Locale.ROOT),
                payload.anchor().name(),
                payload.anchor().dimension().identifier());
    }

    public static void clear() {
        CACHE.clear();
        permissions = AnchorSyncPayloads.PermissionSnapshot.empty();
        serverHasSignPort = false;
    }

    public static boolean serverHasSignPort() {
        return serverHasSignPort;
    }

    public static AnchorSyncPayloads.PermissionSnapshot permissions() {
        return permissions;
    }

    public static List<AnchorClient> anchors() {
        return CACHE.values().stream()
                .flatMap(anchors -> anchors.values().stream())
                .toList();
    }

    public static List<AnchorClient> anchors(ResourceKey<Level> dimension) {
        Map<String, AnchorClient> anchors = CACHE.get(dimension);
        if (anchors == null) return List.of();
        return List.copyOf(anchors.values());
    }

    public static Optional<AnchorClient> find(String name, ResourceKey<Level> dimension) {
        Map<String, AnchorClient> anchors = CACHE.get(dimension);
        if (anchors == null) return Optional.empty();
        return Optional.ofNullable(anchors.get(key(name)));
    }

    public static Optional<AnchorClient> findAnyDimension(String name) {
        String key = key(name);
        for (Map<String, AnchorClient> anchors : CACHE.values()) {
            AnchorClient anchor = anchors.get(key);
            if (anchor != null) return Optional.of(anchor);
        }
        return Optional.empty();
    }

    private static void upsert(AnchorClient anchor) {
        CACHE.computeIfAbsent(anchor.dimension(), ignored -> new HashMap<>())
                .put(key(anchor.name()), anchor);
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
