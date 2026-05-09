package tech.endorsed.signport.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import tech.endorsed.signport.SignPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AnchorState extends PersistentState {

    public AnchorState() { }

    public AnchorState(List<Anchor> anchors) {
        this.anchors = anchors;
    }

    public List<Anchor> anchors = new ArrayList<>();
    private transient Map<String, Anchor> anchorsByName;
    private transient Map<String, Anchor> anchorsByLowercaseName;
    private transient int lookupSize = -1;

    public List<Anchor> GetAnchors() {
        return anchors;
    }

    public Optional<Anchor> findAnchor(String name) {
        if (name == null) return Optional.empty();

        rebuildLookupIfNeeded();
        return Optional.ofNullable(anchorsByName.get(name));
    }

    public Optional<Anchor> findAnchorIgnoreCase(String name) {
        if (name == null) return Optional.empty();

        rebuildLookupIfNeeded();
        return Optional.ofNullable(anchorsByLowercaseName.get(name.toLowerCase(Locale.ROOT)));
    }

    public void addAnchor(Anchor anchor) {
        anchors.add(anchor);
        invalidateLookup();
        markDirty();
    }

    public boolean deleteAnchor(String name) {
        for (int index = 0; index < anchors.size(); index++) {
            if (anchors.get(index).name.equals(name)) {
                anchors.remove(index);
                invalidateLookup();
                markDirty();
                return true;
            }
        }

        return false;
    }

    public void clearAnchors() {
        anchors.clear();
        invalidateLookup();
        markDirty();
    }

    private void rebuildLookupIfNeeded() {
        if (anchorsByName != null && lookupSize == anchors.size()) return;

        anchorsByName = new HashMap<>();
        anchorsByLowercaseName = new HashMap<>();
        for (Anchor anchor : anchors) {
            anchorsByName.putIfAbsent(anchor.name, anchor);
            anchorsByLowercaseName.putIfAbsent(anchor.name.toLowerCase(Locale.ROOT), anchor);
        }
        lookupSize = anchors.size();
    }

    private void invalidateLookup() {
        anchorsByName = null;
        anchorsByLowercaseName = null;
        lookupSize = -1;
    }

    private static final Codec<BlockPos> POS_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("xPos").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("yPos").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("zPos").forGetter(BlockPos::getZ)).apply(inst, BlockPos::new));

    private static final Codec<AnchorState> CODEC = Codec.unboundedMap(Codec.STRING, POS_CODEC).xmap(
    l -> {
        var anchors = new ArrayList<Anchor>();
        for (var entry : l.entrySet()) {
            anchors.add(new Anchor(entry.getKey(), entry.getValue()));
        }
        return new AnchorState(anchors);
    },
    as -> {
        var entries = new HashMap<String, BlockPos>();
        for (var anchor : as.anchors) {
            entries.put(anchor.name, anchor.pos);
        }
        return entries;
    }).fieldOf("anchors").codec();

    private static final PersistentStateType<AnchorState> type = new PersistentStateType<>(SignPort.MOD_ID, AnchorState::new, CODEC, DataFixTypes.CHUNK);

    public static AnchorState getServerState(ServerWorld world) {
        if (world == null) return null;
        PersistentStateManager persistentStateManager = world.getPersistentStateManager();

        return persistentStateManager.getOrCreate(type);
    }
}
