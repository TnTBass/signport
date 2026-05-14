package tech.endorsed.signport.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.client.AnchorClient;
import tech.endorsed.signport.client.SignPortClientState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AnchorBrowserScreen extends Screen {
    private static final Map<String, Boolean> COLLAPSED_GROUPS = new HashMap<>();
    private static final int PANEL_WIDTH = 360;
    private static final int ROW_HEIGHT = 22;
    private static final int GROUP_HEIGHT = 18;
    private static final int TAB_HEIGHT = 20;

    private final Screen parent;
    private EditBox searchBox;
    private SortMode sortMode = SortMode.NAME;
    private ResourceKey<Level> selectedDimension;
    private List<RowHit> rowHits = List.of();
    private List<GroupHit> groupHits = List.of();
    private List<TabHit> tabHits = List.of();

    public AnchorBrowserScreen(Screen parent) {
        super(Component.literal("SignPort Anchors"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        this.searchBox = new EditBox(this.font, left, top + 36, PANEL_WIDTH - 112, 18, Component.literal("Search anchors"));
        this.searchBox.setHint(Component.literal("Search"));
        this.searchBox.setResponder(ignored -> rebuildRows());
        this.addRenderableWidget(searchBox);

        CycleButton<SortMode> sortButton = CycleButton.builder(SortMode::label, sortMode)
                .withValues(SortMode.values())
                .create(left + PANEL_WIDTH - 104, top + 36, 104, 18, Component.literal("Sort"), (button, value) -> {
                    sortMode = value;
                    rebuildRows();
                });
        this.addRenderableWidget(sortButton);
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 60, top + 6, 60, 20)
                .build());

        if (this.selectedDimension == null && this.minecraft != null && this.minecraft.level != null) {
            this.selectedDimension = this.minecraft.level.dimension();
        }
        rebuildRows();
        setInitialFocus(searchBox);
    }

    @Override
    public void tick() {
        rebuildRows();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        graphics.fill(left - 12, top - 8, left + PANEL_WIDTH + 12, top + panelHeight(), 0xCC101010);
        graphics.outline(left - 12, top - 8, PANEL_WIDTH + 24, panelHeight() + 8, 0xFF707070);
        graphics.centeredText(this.font, this.title, left + PANEL_WIDTH / 2, top + 10, 0xFFFFFFFF);

        drawTabs(graphics, left, top + 60);
        int y = top + 88;
        if (SignPortClientState.anchors().isEmpty()) {
            graphics.centeredText(this.font, Component.literal("No synced anchors"), left + PANEL_WIDTH / 2, y + 24, 0xFFAAAAAA);
            return;
        }

        for (GroupHit group : groupHits) {
            boolean collapsed = isCollapsed(group.key());
            graphics.fill(left, group.y(), left + PANEL_WIDTH, group.y() + GROUP_HEIGHT, 0xFF242424);
            graphics.text(this.font, (collapsed ? "+ " : "- ") + group.label() + " (" + group.count() + ")", left + 6, group.y() + 5, 0xFFE0E0E0);
        }

        for (RowHit row : rowHits) {
            int color = row.contains(mouseX, mouseY) ? 0xFF333E4A : 0xFF1B1B1B;
            graphics.fill(left, row.y(), left + PANEL_WIDTH, row.y() + ROW_HEIGHT - 1, color);
            AnchorClient anchor = row.anchor();
            graphics.text(this.font, rowTitle(anchor), left + 6, row.y() + 4, 0xFFFFFFFF);
            graphics.text(this.font, rowMeta(anchor), left + 118, row.y() + 4, 0xFFB8B8B8);
            if (showRawTeleportButton()) {
                graphics.fill(row.teleportX(), row.y() + 3, row.teleportX() + 58, row.y() + 17, 0xFF29445F);
                graphics.outline(row.teleportX(), row.y() + 3, 58, 14, 0xFF5988B8);
                graphics.centeredText(this.font, Component.literal("teleport"), row.teleportX() + 29, row.y() + 6, 0xFFFFFFFF);
            }
        }
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int left, int y) {
        tabHits = dimensionsWithCounts().stream().collect(ArrayList::new, (hits, tab) -> {
            int x = left + hits.stream().mapToInt(TabHit::width).sum();
            int width = Math.max(72, Math.min(138, this.font.width(tab.label()) + 22));
            boolean selected = tab.dimension().equals(selectedDimension);
            graphics.fill(x, y, x + width - 3, y + TAB_HEIGHT, selected ? 0xFF3C4B5D : 0xFF222222);
            graphics.outline(x, y, width - 3, TAB_HEIGHT, selected ? 0xFF7DA7D9 : 0xFF555555);
            graphics.centeredText(this.font, Component.literal(tab.label()), x + (width - 3) / 2, y + 6, 0xFFFFFFFF);
            hits.add(new TabHit(tab.dimension(), x, y, width, TAB_HEIGHT));
        }, List::addAll);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        for (TabHit tab : tabHits) {
            if (tab.contains(mouseX, mouseY)) {
                selectedDimension = tab.dimension();
                rebuildRows();
                return true;
            }
        }
        for (GroupHit group : groupHits) {
            if (group.contains(mouseX, mouseY)) {
                COLLAPSED_GROUPS.put(group.key(), !isCollapsed(group.key()));
                rebuildRows();
                return true;
            }
        }
        for (RowHit row : rowHits) {
            if (row.contains(mouseX, mouseY)) {
                if (showRawTeleportButton() && row.teleportContains(mouseX, mouseY)) {
                    sendCommand("tp @s %d %d %d".formatted(row.anchor().pos().getX(), row.anchor().pos().getY(), row.anchor().pos().getZ()));
                } else {
                    sendCommand("sp tp " + row.anchor().name());
                }
                onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void rebuildRows() {
        if (this.minecraft != null && this.minecraft.level != null && selectedDimension == null) {
            selectedDimension = this.minecraft.level.dimension();
        }
        if (selectedDimension != null && SignPortClientState.anchors(selectedDimension).isEmpty()) {
            List<DimensionTab> tabs = dimensionsWithCounts();
            if (!tabs.isEmpty()) selectedDimension = tabs.getFirst().dimension();
        }

        String needle = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        List<AnchorClient> anchors = selectedDimension == null ? List.of() : SignPortClientState.anchors(selectedDimension);
        anchors = anchors.stream()
                .filter(anchor -> matches(anchor, needle))
                .sorted(comparator())
                .toList();

        LinkedHashMap<String, List<AnchorClient>> grouped = new LinkedHashMap<>();
        for (AnchorClient anchor : anchors) {
            grouped.computeIfAbsent(groupKey(anchor), ignored -> new ArrayList<>()).add(anchor);
        }

        int y = top() + 88;
        List<GroupHit> groups = new ArrayList<>();
        List<RowHit> rows = new ArrayList<>();
        for (Map.Entry<String, List<AnchorClient>> entry : grouped.entrySet()) {
            groups.add(new GroupHit(entry.getKey(), groupLabel(entry.getKey()), entry.getValue().size(), left(), y, PANEL_WIDTH, GROUP_HEIGHT));
            y += GROUP_HEIGHT;
            if (!isCollapsed(entry.getKey())) {
                for (AnchorClient anchor : entry.getValue()) {
                    rows.add(new RowHit(anchor, left(), y, PANEL_WIDTH, ROW_HEIGHT));
                    y += ROW_HEIGHT;
                }
            }
        }
        groupHits = groups;
        rowHits = rows;
    }

    private Comparator<AnchorClient> comparator() {
        return switch (sortMode) {
            case DISTANCE -> Comparator
                    .comparingDouble((AnchorClient anchor) -> playerPos().distSqr(anchor.pos()))
                    .thenComparing(AnchorClient::name, String.CASE_INSENSITIVE_ORDER);
            case RECENT -> (first, second) -> {
                boolean firstLegacy = first.createdAt() == 0L;
                boolean secondLegacy = second.createdAt() == 0L;
                if (firstLegacy && secondLegacy) return String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name());
                if (firstLegacy) return 1;
                if (secondLegacy) return -1;
                int recent = Long.compare(second.createdAt(), first.createdAt());
                return recent != 0 ? recent : String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name());
            };
            case NAME -> Comparator
                    .comparing((AnchorClient anchor) -> groupKey(anchor), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AnchorClient::name, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private List<DimensionTab> dimensionsWithCounts() {
        return SignPortClientState.anchors().stream()
                .collect(LinkedHashMap<ResourceKey<Level>, Integer>::new,
                        (counts, anchor) -> counts.merge(anchor.dimension(), 1, Integer::sum),
                        LinkedHashMap::putAll)
                .entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString()))
                .map(entry -> new DimensionTab(entry.getKey(), entry.getKey().identifier() + " (" + entry.getValue() + ")"))
                .toList();
    }

    private boolean matches(AnchorClient anchor, String needle) {
        if (needle.isBlank()) return true;
        return anchor.name().toLowerCase(Locale.ROOT).contains(needle)
                || groupKey(anchor).toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean showRawTeleportButton() {
        return SignPortClientState.permissions().canDeleteAnchor();
    }

    private void sendCommand(String command) {
        Minecraft client = this.minecraft;
        if (client != null && client.player != null) {
            client.player.connection.sendCommand(command);
        }
    }

    private String rowTitle(AnchorClient anchor) {
        return anchor.group() == null || anchor.group().isBlank() ? anchor.name() : anchor.group() + ": " + anchor.name();
    }

    private String rowMeta(AnchorClient anchor) {
        BlockPos pos = anchor.pos();
        return "%d %d %d · %dm".formatted(pos.getX(), pos.getY(), pos.getZ(),
                Math.round(Math.sqrt(playerPos().distSqr(pos))));
    }

    private BlockPos playerPos() {
        return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.blockPosition() : BlockPos.ZERO;
    }

    private boolean isCollapsed(String group) {
        return COLLAPSED_GROUPS.getOrDefault(group, false);
    }

    private String groupKey(AnchorClient anchor) {
        return anchor.group() == null || anchor.group().isBlank() ? "" : anchor.group();
    }

    private String groupLabel(String group) {
        return group.isBlank() ? "Ungrouped" : group;
    }

    private int left() {
        return Math.max(16, (this.width - PANEL_WIDTH) / 2);
    }

    private int top() {
        return 24;
    }

    private int panelHeight() {
        return Math.max(170, Math.min(this.height - 48, 112 + rowHits.size() * ROW_HEIGHT + groupHits.size() * GROUP_HEIGHT));
    }

    private enum SortMode {
        NAME("name"),
        DISTANCE("distance"),
        RECENT("recent");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        Component label() {
            return Component.literal(label);
        }
    }

    private record DimensionTab(ResourceKey<Level> dimension, String label) {
    }

    private record TabHit(ResourceKey<Level> dimension, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record GroupHit(String key, String label, int count, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record RowHit(AnchorClient anchor, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        int teleportX() {
            return x + width - 64;
        }

        boolean teleportContains(double mouseX, double mouseY) {
            return mouseX >= teleportX() && mouseX < teleportX() + 58 && mouseY >= y + 3 && mouseY < y + 17;
        }
    }
}
