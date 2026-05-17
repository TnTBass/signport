package tech.endorsed.signport.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import tech.endorsed.signport.SignPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public class AnchorState extends SavedData {

    public AnchorState() { }

    public AnchorState(List<Anchor> anchors) {
        this.anchors = new ArrayList<>(anchors);
    }

    public List<Anchor> anchors = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Queries — all dimension-scoped so names only need to be unique per dimension
    // -------------------------------------------------------------------------

    public Optional<Anchor> findAnchor(String name, ResourceKey<Level> dimension) {
        if (name == null) return Optional.empty();
        for (Anchor a : anchors) {
            if (a.dimension.equals(dimension) && a.name.equals(name)) return Optional.of(a);
        }
        return Optional.empty();
    }

    public Optional<Anchor> findAnchorIgnoreCase(String name, ResourceKey<Level> dimension) {
        if (name == null) return Optional.empty();
        String lower = name.toLowerCase(Locale.ROOT);
        for (Anchor a : anchors) {
            if (a.dimension.equals(dimension) && a.name.toLowerCase(Locale.ROOT).equals(lower))
                return Optional.of(a);
        }
        return Optional.empty();
    }

    public List<Anchor> getAnchorsForDimension(ResourceKey<Level> dimension) {
        return anchors.stream().filter(a -> a.dimension.equals(dimension)).toList();
    }

    public List<Anchor> getAnchorsByGroup(ResourceKey<Level> dimension, String group) {
        String normalizedGroup = normalizeGroup(group);
        return anchors.stream()
                .filter(a -> a.dimension.equals(dimension) && normalizeGroup(a.group).equals(normalizedGroup))
                .toList();
    }

    public SortedSet<String> getGroupsForDimension(ResourceKey<Level> dimension) {
        SortedSet<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        anchors.stream()
                .filter(a -> a.dimension.equals(dimension))
                .map(a -> normalizeGroup(a.group))
                .filter(group -> !group.isEmpty())
                .forEach(groups::add);
        return groups;
    }

    private static String normalizeGroup(String group) {
        return AnchorCreation.normalizeGroup(group);
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    public void addAnchor(Anchor anchor) {
        if (anchor.createdAt == 0L) {
            anchor.createdAt = System.currentTimeMillis();
        }
        anchors.add(anchor);
        setDirty();
    }

    public boolean deleteAnchor(String name, ResourceKey<Level> dimension) {
        for (int i = 0; i < anchors.size(); i++) {
            Anchor a = anchors.get(i);
            if (a.dimension.equals(dimension) && a.name.equals(name)) {
                anchors.remove(i);
                setDirty();
                return true;
            }
        }
        return false;
    }

    public void clearAnchors(ResourceKey<Level> dimension) {
        if (anchors.removeIf(a -> a.dimension.equals(dimension))) setDirty();
    }

    public boolean setAnchorGroup(String name, ResourceKey<Level> dimension, String group) {
        Optional<Anchor> anchor = findAnchor(name, dimension);
        if (anchor.isEmpty()) return false;
        anchor.get().group = normalizeGroup(group);
        setDirty();
        return true;
    }

    // -------------------------------------------------------------------------
    // Codec — writes v2_anchors; on load falls back to legacy "anchors" map
    // (overworld-only, no dimension field) and re-saves in v2 format.
    // -------------------------------------------------------------------------

    private static final Codec<BlockPos> POS_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("xPos").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("yPos").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("zPos").forGetter(BlockPos::getZ))
            .apply(inst, BlockPos::new));

    private static final Codec<Anchor> ANCHOR_V2_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("name").forGetter(a -> a.name),
            POS_CODEC.fieldOf("pos").forGetter(a -> a.pos),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(a -> a.dimension),
            Codec.STRING.optionalFieldOf("group", "").forGetter(Anchor::group),
            Codec.LONG.optionalFieldOf("createdAt", 0L).forGetter(Anchor::createdAt))
            .apply(inst, Anchor::new));

    // Legacy map codec — used only to read old "anchors" field during migration
    private static final Codec<Map<String, BlockPos>> LEGACY_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, POS_CODEC);

    private static final MapCodec<AnchorState> STATE_MAP_CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("v2_anchors"));
        }

        @Override
        public <T> DataResult<AnchorState> decode(DynamicOps<T> ops, MapLike<T> input) {
            List<Anchor> anchors = new ArrayList<>();

            // New format
            T v2Field = input.get("v2_anchors");
            if (v2Field != null) {
                ANCHOR_V2_CODEC.listOf().parse(ops, v2Field).result().ifPresent(anchors::addAll);
            }

            boolean legacy = false;
            // Legacy fallback — overworld anchors saved before dimension tracking
            if (anchors.isEmpty()) {
                T legacyField = input.get("anchors");
                if (legacyField != null) {
                    Optional<Map<String, BlockPos>> legacyMap =
                            LEGACY_MAP_CODEC.parse(ops, legacyField).result();
                    if (legacyMap.isPresent() && !legacyMap.get().isEmpty()) {
                        for (var entry : legacyMap.get().entrySet()) {
                            anchors.add(new Anchor(entry.getKey(), entry.getValue(), Level.OVERWORLD));
                        }
                        legacy = true;
                    }
                }
            }

            if (legacy) {
                SignPort.LOGGER.info("[Signport] Old anchor data discovered. Migrating to Minecraft 26.1 world format.");
            }

            AnchorState state = new AnchorState(anchors);
            if (legacy) state.setDirty(); // triggers immediate resave in v2 format
            return DataResult.success(state);
        }

        @Override
        public <T> RecordBuilder<T> encode(AnchorState state, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix.add("v2_anchors", ANCHOR_V2_CODEC.listOf().encodeStart(ops, state.anchors));
        }
    };

    static final Codec<AnchorState> CODEC = STATE_MAP_CODEC.codec();

    private static final SavedDataType<AnchorState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SignPort.MOD_ID, SignPort.MOD_ID),
            AnchorState::new,
            CODEC,
            DataFixTypes.CHUNK);

    // -------------------------------------------------------------------------
    // Storage access — always reads/writes on the overworld so there is exactly
    // one signport.dat per world, at <world>/data/signport.dat.
    // -------------------------------------------------------------------------

    /**
     * Returns the global AnchorState, creating it if it doesn't yet exist.
     * Use for write operations. Also handles one-time migration of legacy
     * per-dimension files left behind by Minecraft's 26.1 world format change.
     */
    public static AnchorState getServerState(MinecraftServer server) {
        AnchorState state = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        migrateLegacyDimensionFiles(server, state);
        return state;
    }

    /**
     * Returns the global AnchorState if one has already been saved, or empty if no
     * anchors have ever been created. Never creates or registers an empty state,
     * so it won't produce a signport.dat file just from reading.
     */
    public static Optional<AnchorState> peekServerState(MinecraftServer server) {
        if (server == null) return Optional.empty();
        return Optional.ofNullable(server.overworld().getDataStorage().get(TYPE));
    }

    // -------------------------------------------------------------------------
    // One-time migration of DIM-1/data and DIM1/data files left behind when
    // Minecraft reorganised world storage in 26.1.
    // -------------------------------------------------------------------------

    /** Guard so migration only runs once per server instance. */
    private boolean legacyFilesChecked = false;

    /** Deletes a directory only if it is empty; silently ignores failures. */
    private static void tryDeleteIfEmpty(Path dir) {
        try {
            Files.delete(dir);
        } catch (IOException ignored) {
            // Non-empty or inaccessible — leave it alone
        }
    }

    private static void migrateLegacyDimensionFiles(MinecraftServer server, AnchorState state) {
        if (state.legacyFilesChecked) return;
        state.legacyFilesChecked = true;

        // cleanupParents: true for DIM-N paths whose parent dirs can be pruned when empty;
        // false for paths whose parents (e.g. data/) are shared and must never be deleted.
        record LegacyEntry(String relativePath, ResourceKey<Level> dimension, boolean cleanupParents) {}
        List<LegacyEntry> candidates = List.of(
                new LegacyEntry("data/signport.dat", Level.OVERWORLD, false),
                new LegacyEntry("DIM-1/data/signport.dat", Level.NETHER, true),
                new LegacyEntry("DIM1/data/signport.dat", Level.END, true));

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        boolean anyMigrated = false;

        for (LegacyEntry entry : candidates) {
            Path legacyFile = worldRoot.resolve(entry.relativePath());
            if (!Files.exists(legacyFile)) continue;

            try {
                CompoundTag nbt = NbtIo.readCompressed(legacyFile, NbtAccounter.unlimitedHeap());
                CompoundTag dataTag = nbt.getCompoundOrEmpty("data");

                Optional<Map<String, BlockPos>> anchorsOpt =
                        LEGACY_MAP_CODEC.fieldOf("anchors").codec()
                                .parse(NbtOps.INSTANCE, dataTag)
                                .result();

                if (anchorsOpt.isEmpty()) {
                    SignPort.LOGGER.error("[Signport] Could not parse legacy anchor data from {}; file left in place.",
                            legacyFile);
                    continue;
                }

                int count = 0;
                for (var kv : anchorsOpt.get().entrySet()) {
                    if (state.findAnchor(kv.getKey(), entry.dimension()).isEmpty()) {
                        state.anchors.add(new Anchor(kv.getKey(), kv.getValue(), entry.dimension()));
                        count++;
                    }
                }
                if (count > 0) {
                    state.setDirty();
                }

                // Remove the legacy file only after a successful parse. For DIM-N paths,
                // also prune the now-empty parent directories (best-effort).
                Files.delete(legacyFile);
                if (entry.cleanupParents()) {
                    tryDeleteIfEmpty(legacyFile.getParent());             // e.g. DIM-1/data
                    tryDeleteIfEmpty(legacyFile.getParent().getParent()); // e.g. DIM-1
                }

                SignPort.LOGGER.info("[Signport] Migrated {} anchor(s) from legacy path: {}",
                        count, legacyFile);
                anyMigrated = true;

            } catch (IOException e) {
                SignPort.LOGGER.error("[Signport] Failed to read legacy anchor data from {}", legacyFile, e);
            }
        }

        if (anyMigrated) {
            SignPort.LOGGER.info("[Signport] Old anchor data discovered. Migrating to Minecraft 26.1 world format.");
        }
    }
}
