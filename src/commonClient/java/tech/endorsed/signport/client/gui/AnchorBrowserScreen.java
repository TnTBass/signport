package tech.endorsed.signport.client.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.client.AnchorClient;
import tech.endorsed.signport.client.SignPortClientState;
import tech.endorsed.signport.network.AnchorSyncPayloads;
import tech.endorsed.signport.world.AnchorCreation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AnchorBrowserScreen extends Screen {
    private static final Map<String, Boolean> COLLAPSED_GROUPS = new HashMap<>();
    private static final int PANEL_WIDTH = 360;
    private static final int ROW_HEIGHT = 22;
    private static final int GROUP_HEIGHT = 18;
    private static final int TAB_HEIGHT = 20;
    private static final int CONTENT_TOP_OFFSET = 88;
    private static final int CONTENT_BOTTOM_PADDING = 8;
    private static CreateAnchorSender createAnchorSender =
            payload -> SignPort.LOGGER.warn("[SignPort] Cannot create anchor; no client networking sender is installed.");

    private final Screen parent;
    private EditBox searchBox;
    private SortMode sortMode = SortMode.NAME;
    private ResourceKey<Level> selectedDimension;
    private List<RowHit> rowHits = List.of();
    private List<GroupHit> groupHits = List.of();
    private List<TabHit> tabHits = List.of();
    private int scrollOffset = 0;
    private Button createButton;
    private Button createSubmitButton;
    private Button createCancelButton;
    private EditBox createNameField;
    private EditBox createGroupField;
    private boolean createDialogOpen = false;
    private boolean createPending = false;
    private boolean createServerRejected = false;
    private String createStatusMessage = "";
    private ValidationState createValidation = ValidationState.RED;
    private List<String> createGroupSuggestions = List.of();
    private int selectedGroupSuggestion = 0;

    public AnchorBrowserScreen(Screen parent) {
        super(Component.literal("SignPort Anchors"));
        this.parent = parent;
    }

    public static void installCreateAnchorSender(CreateAnchorSender sender) {
        createAnchorSender = sender == null ? payload -> {
        } : sender;
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        this.searchBox = new EditBox(this.font, left, top + 36, PANEL_WIDTH - 192, 18, Component.literal("Search anchors"));
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
        this.createButton = this.addRenderableWidget(Button.builder(Component.literal("+ Create"), button -> openCreateDialog())
                .bounds(left + PANEL_WIDTH - 184, top + 36, 76, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 60, top + 6, 60, 20)
                .build());
        this.createNameField = this.addRenderableWidget(new EditBox(this.font, left + 150, top + 92, 150, 18, Component.literal("Anchor name")));
        this.createNameField.setMaxLength(AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
        this.createNameField.setResponder(ignored -> handleCreateInputChanged());
        this.createGroupField = this.addRenderableWidget(new EditBox(this.font, left + 150, top + 120, 150, 18, Component.literal("Group")));
        this.createGroupField.setResponder(ignored -> handleCreateInputChanged());
        this.createSubmitButton = this.addRenderableWidget(Button.builder(Component.literal("Create"), button -> submitCreateDialog())
                .bounds(left + 162, top + 192, 66, 20)
                .build());
        this.createCancelButton = this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> closeCreateDialog())
                .bounds(left + 234, top + 192, 66, 20)
                .build());
        updateCreateWidgetVisibility();

        if (this.selectedDimension == null && this.minecraft != null && this.minecraft.level != null) {
            this.selectedDimension = this.minecraft.level.dimension();
        }
        rebuildRows();
        setInitialFocus(searchBox);
    }

    @Override
    public void tick() {
        rebuildRows();
        if (createDialogOpen) updateCreateValidation();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = left();
        int top = top();
        graphics.fill(left - 12, top - 8, left + PANEL_WIDTH + 12, top + panelHeight(), 0xCC101010);
        graphics.outline(left - 12, top - 8, PANEL_WIDTH + 24, panelHeight() + 8, 0xFF707070);
        graphics.centeredText(this.font, this.title, left + PANEL_WIDTH / 2, top + 10, 0xFFFFFFFF);

        drawTabs(graphics, left, top + 60);
        int y = top + CONTENT_TOP_OFFSET;
        if (SignPortClientState.anchors().isEmpty()) {
            graphics.centeredText(this.font, Component.literal("No synced anchors"), left + PANEL_WIDTH / 2, y + 24, 0xFFAAAAAA);
        } else {
            int contentTop = contentTop();
            int contentBottom = contentBottom();
            graphics.enableScissor(left, contentTop, left + PANEL_WIDTH, contentBottom);
            for (GroupHit group : groupHits) {
                if (group.y() + GROUP_HEIGHT <= contentTop || group.y() >= contentBottom) continue;
                boolean collapsed = isCollapsed(group.key());
                graphics.fill(left, group.y(), left + PANEL_WIDTH, group.y() + GROUP_HEIGHT, 0xFF242424);
                graphics.text(this.font, (collapsed ? "+ " : "- ") + group.label() + " (" + group.count() + ")", left + 6, group.y() + 5, 0xFFE0E0E0);
            }

            for (RowHit row : rowHits) {
                if (row.y() + ROW_HEIGHT <= contentTop || row.y() >= contentBottom) continue;
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
            graphics.disableScissor();
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (createDialogOpen) {
            renderCreateDialog(graphics, mouseX, mouseY);
        }
    }

    private void renderCreateDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = left() + 58;
        int top = top() + 64;
        int width = 264;
        int height = 156;
        graphics.fill(left, top, left + width, top + height, 0xEE111111);
        graphics.outline(left, top, width, height, 0xFF7DA7D9);
        graphics.text(this.font, "Create anchor here", left + 10, top + 10, 0xFFFFFFFF);
        graphics.text(this.font, "Name", left + 10, top + 32, 0xFFAAAAAA);
        graphics.text(this.font, "Group", left + 10, top + 60, 0xFFAAAAAA);
        graphics.text(this.font, liveLocationLabel(), left + 10, top + 84, 0xFFAAAAAA);
        graphics.text(this.font, createStatusMessage, left + 10, top + 108, createValidation.color);
        createNameField.extractRenderState(graphics, mouseX, mouseY, 0);
        createGroupField.extractRenderState(graphics, mouseX, mouseY, 0);
        createSubmitButton.extractRenderState(graphics, mouseX, mouseY, 0);
        createCancelButton.extractRenderState(graphics, mouseX, mouseY, 0);
        renderCreateGroupSuggestions(graphics, mouseX, mouseY);
    }

    private void renderCreateGroupSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (createGroupSuggestions.isEmpty() || createGroupField == null || !createGroupField.isFocused()) return;
        int x = createGroupField.getX();
        int y = createGroupField.getY() + createGroupField.getHeight() + 2;
        int width = createGroupField.getWidth();
        int height = createGroupSuggestions.size() * 16;
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xEE111111);
        graphics.outline(x - 2, y - 2, width + 4, height + 4, 0xFF555555);
        for (int i = 0; i < createGroupSuggestions.size(); i++) {
            int rowY = y + i * 16;
            boolean selected = i == selectedGroupSuggestion;
            graphics.fill(x, rowY, x + width, rowY + 16, selected ? 0xFF3C4B5D : 0xFF222222);
            graphics.text(this.font, createGroupSuggestions.get(i), x + 4, rowY + 4, 0xFFFFFFFF);
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
        if (createDialogOpen) {
            if (handleCreateSuggestionClick(mouseX, mouseY)) return true;
            if (createNameField.isMouseOver(mouseX, mouseY)) {
                focusCreateField(createNameField);
                createNameField.mouseClicked(event, doubleClick);
                return true;
            }
            if (createGroupField.isMouseOver(mouseX, mouseY)) {
                focusCreateField(createGroupField);
                createGroupField.mouseClicked(event, doubleClick);
                return true;
            }
            createSubmitButton.mouseClicked(event, doubleClick);
            createCancelButton.mouseClicked(event, doubleClick);
            return true;
        }
        for (TabHit tab : tabHits) {
            if (tab.contains(mouseX, mouseY)) {
                selectedDimension = tab.dimension();
                rebuildRows();
                return true;
            }
        }
        int contentTop = contentTop();
        int contentBottom = contentBottom();
        if (mouseY >= contentTop && mouseY < contentBottom) {
            for (GroupHit group : groupHits) {
                if (group.y() + GROUP_HEIGHT <= contentTop || group.y() >= contentBottom) continue;
                if (group.contains(mouseX, mouseY)) {
                    COLLAPSED_GROUPS.put(group.key(), !isCollapsed(group.key()));
                    rebuildRows();
                    return true;
                }
            }
            for (RowHit row : rowHits) {
                if (row.y() + ROW_HEIGHT <= contentTop || row.y() >= contentBottom) continue;
                if (row.contains(mouseX, mouseY)) {
                    if (showRawTeleportButton() && row.teleportContains(mouseX, mouseY)) {
                        sendCommand("execute in %s run tp @s %d %d %d".formatted(
                                row.anchor().dimension().identifier(),
                                row.anchor().pos().getX(), row.anchor().pos().getY(), row.anchor().pos().getZ()));
                    } else {
                        sendCommand(rowClickCommand(row.anchor()));
                    }
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int newOffset = Math.max(0, Math.min(maxScroll(), scrollOffset + (int) (-scrollY * ROW_HEIGHT)));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            rebuildRows();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (createDialogOpen) {
            if (event.key() == 256) {
                closeCreateDialog();
                return true;
            }
            if (event.key() == 257 || event.key() == 335) {
                submitCreateDialog();
                return true;
            }
            if (createGroupField.isFocused() && !createGroupSuggestions.isEmpty()) {
                if (event.key() == 264) {
                    selectedGroupSuggestion = Math.min(createGroupSuggestions.size() - 1, selectedGroupSuggestion + 1);
                    return true;
                }
                if (event.key() == 265) {
                    selectedGroupSuggestion = Math.max(0, selectedGroupSuggestion - 1);
                    return true;
                }
                if (event.key() == 258) {
                    applySelectedGroupSuggestion();
                    return true;
                }
            }
            return createNameField.keyPressed(event) || createGroupField.keyPressed(event) || super.keyPressed(event);
        }
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (createDialogOpen) {
            return createNameField.charTyped(event) || createGroupField.charTyped(event);
        }
        return super.charTyped(event);
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

        int contentHeight = grouped.entrySet().stream()
                .mapToInt(e -> GROUP_HEIGHT + (isCollapsed(e.getKey()) ? 0 : e.getValue().size() * ROW_HEIGHT))
                .sum();
        int nextPanelHeight = panelHeightForContent(this.height, contentHeight);
        int newMaxScroll = maxScrollForContent(contentHeight, nextPanelHeight);
        scrollOffset = Math.max(0, Math.min(newMaxScroll, scrollOffset));

        int y = top() + CONTENT_TOP_OFFSET - scrollOffset;
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

    public void handleCreateAnchorResponse(AnchorSyncPayloads.CreateAnchorResponse response) {
        if (!createDialogOpen || !createPending) return;
        createPending = false;
        if (response.success()) {
            closeCreateDialog();
            rebuildRows();
            return;
        }
        createStatusMessage = response.errorMessage();
        createServerRejected = true;
        updateCreateValidation();
    }

    private void handleCreateInputChanged() {
        if (createServerRejected) {
            createServerRejected = false;
        }
        updateCreateValidation();
    }

    private boolean canCreateAnchor() {
        return SignPortClientState.serverHasSignPort() && SignPortClientState.permissions().canCreateAnchor();
    }

    private void openCreateDialog() {
        if (!canCreateAnchor()) return;
        createDialogOpen = true;
        createPending = false;
        createServerRejected = false;
        createStatusMessage = "";
        createNameField.setValue("");
        createGroupField.setValue("");
        selectedGroupSuggestion = 0;
        updateCreateValidation();
        updateCreateWidgetVisibility();
        focusCreateField(createNameField);
    }

    private void closeCreateDialog() {
        createDialogOpen = false;
        createPending = false;
        createServerRejected = false;
        createStatusMessage = "";
        createNameField.setFocused(false);
        createGroupField.setFocused(false);
        updateCreateWidgetVisibility();
    }

    private void submitCreateDialog() {
        updateCreateValidation();
        if (createValidation == ValidationState.RED || createPending) return;
        createPending = true;
        createServerRejected = false;
        createStatusMessage = "Creating...";
        updateCreateWidgetVisibility();
        createAnchorSender.send(new AnchorSyncPayloads.CreateAnchorRequest(
                createNameField.getValue().trim(),
                createGroupField.getValue()));
    }

    private void updateCreateValidation() {
        if (createNameField == null || createGroupField == null) return;
        String name = createNameField.getValue().trim();
        // Keep suggestions live while displaying a server rejection so editing can recover inline.
        createGroupSuggestions = groupSuggestions(createGroupField.getValue());
        selectedGroupSuggestion = Math.min(selectedGroupSuggestion, Math.max(0, createGroupSuggestions.size() - 1));
        ResourceKey<Level> createDimension = currentDimension();

        if (createServerRejected) {
            createValidation = ValidationState.RED;
            updateCreateWidgetVisibility();
            return;
        }
        switch (AnchorCreateValidation.validate(name, createDimension)) {
            case RED -> {
                createValidation = ValidationState.RED;
                createStatusMessage = isInvalidCreateName(name, createDimension)
                        ? "Enter an anchor name"
                        : "Name already exists in this dimension";
            }
            case ORANGE -> {
                createValidation = ValidationState.ORANGE;
                createStatusMessage = "Name exists in another dimension. Signs may need a dimension line.";
            }
            case GREEN -> {
                createValidation = ValidationState.GREEN;
                createStatusMessage = "Ready to create";
            }
        }
        updateCreateWidgetVisibility();
    }

    private boolean isInvalidCreateName(String name, ResourceKey<Level> createDimension) {
        return name.isEmpty() || name.length() > AnchorCreation.MAX_ANCHOR_NAME_LENGTH || createDimension == null;
    }

    private List<String> groupSuggestions(String input) {
        String needle = input == null ? "" : input.toLowerCase(Locale.ROOT);
        Set<String> groups = new LinkedHashSet<>();
        for (AnchorClient anchor : SignPortClientState.anchors()) {
            if (anchor.group() != null && !anchor.group().isBlank()
                    && anchor.group().toLowerCase(Locale.ROOT).contains(needle)) {
                groups.add(anchor.group());
            }
        }
        return groups.stream().limit(5).toList();
    }

    private ResourceKey<Level> currentDimension() {
        return this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.dimension() : null;
    }

    private boolean handleCreateSuggestionClick(double mouseX, double mouseY) {
        if (createGroupSuggestions.isEmpty() || createGroupField == null || !createGroupField.isFocused()) return false;
        int x = createGroupField.getX();
        int y = createGroupField.getY() + createGroupField.getHeight() + 2;
        int width = createGroupField.getWidth();
        for (int i = 0; i < createGroupSuggestions.size(); i++) {
            int rowY = y + i * 16;
            if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + 16) {
                selectedGroupSuggestion = i;
                applySelectedGroupSuggestion();
                return true;
            }
        }
        return false;
    }

    private void applySelectedGroupSuggestion() {
        if (createGroupSuggestions.isEmpty()) return;
        createGroupField.setValue(createGroupSuggestions.get(selectedGroupSuggestion));
        focusCreateField(createGroupField);
        updateCreateValidation();
    }

    private void focusCreateField(EditBox field) {
        setFocused(field);
        createNameField.setFocused(field == createNameField);
        createGroupField.setFocused(field == createGroupField);
    }

    private void updateCreateWidgetVisibility() {
        if (createButton != null) {
            createButton.visible = canCreateAnchor();
            createButton.active = canCreateAnchor() && !createDialogOpen;
        }
        if (createNameField == null) return;
        createNameField.visible = createDialogOpen;
        createNameField.active = createDialogOpen && !createPending;
        createGroupField.visible = createDialogOpen;
        createGroupField.active = createDialogOpen && !createPending;
        createSubmitButton.visible = createDialogOpen;
        createSubmitButton.active = createDialogOpen && !createPending && createValidation != ValidationState.RED;
        createCancelButton.visible = createDialogOpen;
        createCancelButton.active = createDialogOpen;
    }

    private String liveLocationLabel() {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            return "Current location unavailable";
        }
        BlockPos pos = this.minecraft.player.blockPosition();
        return "%s %d %d %d".formatted(
                this.minecraft.level.dimension().identifier(),
                pos.getX(), pos.getY(), pos.getZ());
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

    static String rowTitle(AnchorClient anchor) {
        return anchor.name();
    }

    static String rowClickCommand(AnchorClient anchor) {
        return "execute in %s run sp tp %s".formatted(anchor.dimension().identifier(), commandString(anchor.name()));
    }

    private static String commandString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
        return panelHeightForContent(this.height, rowHits.size() * ROW_HEIGHT + groupHits.size() * GROUP_HEIGHT);
    }

    private int contentTop() {
        return top() + CONTENT_TOP_OFFSET;
    }

    private int contentBottom() {
        return top() + panelHeight() - CONTENT_BOTTOM_PADDING;
    }

    private int maxScroll() {
        int contentHeight = rowHits.size() * ROW_HEIGHT + groupHits.size() * GROUP_HEIGHT;
        return maxScrollForContent(contentHeight, panelHeight());
    }

    static int panelHeightForContent(int screenHeight, int contentHeight) {
        return Math.max(170, Math.min(screenHeight - 48, 112 + contentHeight));
    }

    static int maxScrollForContent(int contentHeight, int panelHeight) {
        return Math.max(0, contentHeight - (panelHeight - CONTENT_TOP_OFFSET - CONTENT_BOTTOM_PADDING));
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

    private enum ValidationState {
        GREEN(0xFF55FF55),
        ORANGE(0xFFFFAA00),
        RED(0xFFFF5555);

        private final int color;

        ValidationState(int color) {
            this.color = color;
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

    public interface CreateAnchorSender {
        void send(AnchorSyncPayloads.CreateAnchorRequest payload);
    }
}
