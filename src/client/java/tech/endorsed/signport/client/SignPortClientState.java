package tech.endorsed.signport.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.network.AnchorSyncPayloads;

import java.util.HashMap;
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
    }

    public static void applyDelta(AnchorSyncPayloads.Delta payload) {
        if (!serverHasSignPort) return;

        if (payload.action() == AnchorSyncPayloads.Action.DELETE) {
            Map<String, AnchorClient> anchors = CACHE.get(payload.dimension());
            if (anchors != null) {
                anchors.remove(key(payload.name()));
            }
            return;
        }

        upsert(AnchorClient.from(payload.anchor()));
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
