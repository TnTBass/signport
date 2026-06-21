package tech.endorsed.signport.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.endorsed.signport.client.AnchorClient;
import tech.endorsed.signport.client.SignPortClientState;
import tech.endorsed.signport.client.config.SignPortClientConfig;
import tech.endorsed.signport.world.PortSignFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    private static final int MAX_SUGGESTIONS = 8;
    private static final int MAX_TEMPLATE_SUGGESTIONS = 6;
    private static final int SUGGESTION_WIDTH = 220;
    private static final int SUGGESTION_ROW_HEIGHT = 14;
    private static final int TEMPLATE_WIDTH = 300;
    private static final int TEMPLATE_HEIGHT = 224;
    private static final int TEMPLATE_FIELD_WIDTH = 220;
    private static final int TEMPLATE_FIELD_HEIGHT = 18;
    private static final int TEMPLATE_ROW_HEIGHT = 14;

    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private String[] messages;
    @Shadow private int line;

    private List<AnchorClient> signportSuggestions = List.of();
    private int signportSelectedSuggestion = 0;
    private int signportScrollOffset = 0;
    private boolean signportDismissed = false;
    private int signportPopupX;
    private int signportPopupY;
    private Button signportTemplateButton;
    private EditBox signportTemplateTargetField;
    private EditBox signportTemplateLabelField;
    private Button signportTemplateDimensionButton;
    private Button signportTemplateApplyButton;
    private Button signportTemplateCancelButton;
    private boolean signportTemplateOpen = false;
    private boolean signportTemplateDimensionOpen = false;
    private List<AnchorClient> signportTemplateSuggestions = List.of();
    private int signportTemplateSelectedSuggestion = 0;
    private int signportTemplateScrollOffset = 0;
    private List<DimensionOption> signportTemplateDimensionOptions = List.of(DimensionOption.DEFAULT);
    private int signportTemplateSelectedDimension = 0;
    private int signportTemplateLeft;
    private int signportTemplateTop;
    private int signportTemplateSuggestionX;
    private int signportTemplateSuggestionY;
    private int signportTemplateDimensionX;
    private int signportTemplateDimensionY;

    @Invoker("setMessage")
    protected abstract void signportSetMessage(String message);

    private <T extends GuiEventListener & Renderable & NarratableEntry> T signportAddRenderableWidget(T widget) {
        return ((ScreenAccessor) (Object) this).signport$addRenderableWidget(widget);
    }

    private void signportSetInitialFocus(GuiEventListener listener) {
        ((ScreenAccessor) (Object) this).signport$setInitialFocus(listener);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void signportAddSuggestionClickTarget(CallbackInfo ci) {
        signportAddRenderableWidget(new SuggestionClickTarget());
        int doneY = ((AbstractSignEditScreen) (Object) this).height / 4 + 144;
        signportTemplateButton = signportAddRenderableWidget(Button.builder(Component.literal("SignPort Template"),
                        button -> openTemplateDialog())
                .bounds(((AbstractSignEditScreen) (Object) this).width / 2 - 100, doneY - 24, 200, 20)
                .build());
        createTemplateWidgets();
        updateTemplateButtonVisibility();
        updateTemplateWidgetVisibility();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void signportHandleSuggestionKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (signportTemplateOpen) {
            signportSuggestions = List.of();
            boolean handled = handleTemplateKeys(event);
            if (!handled && event.key() == 258) {
                if (signportTemplateTargetField.isFocused()) {
                    focusTemplateField(signportTemplateLabelField);
                } else {
                    focusTemplateField(signportTemplateTargetField);
                }
                handled = true;
            }
            if (!handled) {
                handled = signportTemplateTargetField.keyPressed(event) || signportTemplateLabelField.keyPressed(event);
            }
            if (!handled && (event.key() == 257 || event.key() == 335)) {
                applyTemplateDialog();
            }
            refreshTemplateSuggestions();
            updateTemplateWidgetVisibility();
            cir.setReturnValue(true);
            return;
        }
        refreshSuggestions();
        if (signportSuggestions.isEmpty()) return;

        switch (event.key()) {
            case 264 -> {
                signportSelectedSuggestion = (signportSelectedSuggestion + 1) % signportSuggestions.size();
                if (signportSelectedSuggestion == 0) {
                    signportScrollOffset = 0;
                } else if (signportSelectedSuggestion >= signportScrollOffset + MAX_SUGGESTIONS) {
                    signportScrollOffset = signportSelectedSuggestion - MAX_SUGGESTIONS + 1;
                }
                cir.setReturnValue(true);
            }
            case 265 -> {
                signportSelectedSuggestion = (signportSelectedSuggestion + signportSuggestions.size() - 1) % signportSuggestions.size();
                if (signportSelectedSuggestion == signportSuggestions.size() - 1) {
                    signportScrollOffset = Math.max(0, signportSuggestions.size() - MAX_SUGGESTIONS);
                } else if (signportSelectedSuggestion < signportScrollOffset) {
                    signportScrollOffset = signportSelectedSuggestion;
                }
                cir.setReturnValue(true);
            }
            case 258, 257 -> {
                acceptSuggestion(signportSuggestions.get(signportSelectedSuggestion));
                cir.setReturnValue(true);
            }
            case 256 -> {
                signportDismissed = true;
                signportSuggestions = List.of();
                cir.setReturnValue(true);
            }
            default -> {
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void signportAfterKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (signportTemplateOpen) {
            refreshTemplateSuggestions();
            return;
        }
        if (event.key() != 256 && event.key() != 258 && event.key() != 257 && event.key() != 264 && event.key() != 265) {
            signportDismissed = false;
        }
        refreshSuggestions();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void signportHandleTemplateChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (signportTemplateOpen) {
            signportTemplateTargetField.charTyped(event);
            signportTemplateLabelField.charTyped(event);
            refreshTemplateSuggestions();
            updateTemplateWidgetVisibility();
            cir.setReturnValue(true);
            return;
        }
    }

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void signportAfterChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (signportTemplateOpen) return;
        signportDismissed = false;
        refreshSuggestions();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void signportRenderSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        updateTemplateButtonVisibility();
        updateTemplateWidgetVisibility();
        if (signportTemplateOpen) {
            renderTemplateDialog(graphics, mouseX, mouseY);
            return;
        }
        refreshSuggestions();
        if (signportSuggestions.isEmpty()) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int visibleCount = Math.min(MAX_SUGGESTIONS, signportSuggestions.size());
        signportPopupX = Math.max(12, Math.min(width - SUGGESTION_WIDTH - 12, width / 2 - SUGGESTION_WIDTH / 2));
        signportPopupY = Math.min(height - 20 - visibleCount * SUGGESTION_ROW_HEIGHT, height / 2 + 56);

        graphics.nextStratum();
        graphics.fill(signportPopupX - 2, signportPopupY - 2, signportPopupX + SUGGESTION_WIDTH + 2,
                signportPopupY + visibleCount * SUGGESTION_ROW_HEIGHT + 2, 0xDD101010);
        graphics.outline(signportPopupX - 2, signportPopupY - 2, SUGGESTION_WIDTH + 4,
                visibleCount * SUGGESTION_ROW_HEIGHT + 4, 0xFF6F8CAF);

        for (int i = 0; i < visibleCount; i++) {
            int idx = signportScrollOffset + i;
            AnchorClient anchor = signportSuggestions.get(idx);
            int y = signportPopupY + i * SUGGESTION_ROW_HEIGHT;
            if (idx == signportSelectedSuggestion) {
                graphics.fill(signportPopupX, y, signportPopupX + SUGGESTION_WIDTH, y + SUGGESTION_ROW_HEIGHT, 0xFF31465E);
            }
            graphics.text(net.minecraft.client.Minecraft.getInstance().font, suggestionLabel(anchor), signportPopupX + 4, y + 3, 0xFFFFFFFF);
        }
    }

    private boolean handleSuggestionMouseClick(MouseButtonEvent event) {
        if (signportTemplateOpen) {
            return handleTemplateMouseClick(event);
        }
        refreshSuggestions();
        if (signportSuggestions.isEmpty()) return false;

        double mouseX = event.x();
        double mouseY = event.y();
        int visibleCount = Math.min(MAX_SUGGESTIONS, signportSuggestions.size());
        if (mouseX < signportPopupX || mouseX >= signportPopupX + SUGGESTION_WIDTH || mouseY < signportPopupY
                || mouseY >= signportPopupY + visibleCount * SUGGESTION_ROW_HEIGHT) return false;
        int index = (int) ((mouseY - signportPopupY) / SUGGESTION_ROW_HEIGHT) + signportScrollOffset;
        if (index >= 0 && index < signportSuggestions.size()) {
            acceptSuggestion(signportSuggestions.get(index));
            return true;
        }
        return false;
    }

    private void refreshSuggestions() {
        if (!SignPortClientConfig.get().signEditorAutocompleteEnabled || SignPortClientState.anchors().isEmpty()
                || signportTemplateOpen || signportDismissed || line != 2 || !PortSignFormat.isPortalMarker(messages[1])) {
            signportSuggestions = List.of();
            signportSelectedSuggestion = 0;
            signportScrollOffset = 0;
            return;
        }

        String typed = PortSignFormat.normalizeLine(messages[2]);
        String needle = typed.toLowerCase(Locale.ROOT);
        Identifier dimensionId = PortSignFormat.parseDimensionId(messages[3]);
        ResourceKey<Level> dimension = dimensionId == null ? null : ResourceKey.create(Registries.DIMENSION, dimensionId);

        signportSuggestions = SignPortClientState.anchors().stream()
                .filter(anchor -> dimension == null || anchor.dimension().equals(dimension))
                .filter(anchor -> needle.isBlank() || anchor.name().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(suggestionComparator(needle))
                .toList();
        if (signportSelectedSuggestion >= signportSuggestions.size()) {
            signportSelectedSuggestion = 0;
            signportScrollOffset = 0;
        }
        signportScrollOffset = Math.max(0, Math.min(signportScrollOffset, Math.max(0, signportSuggestions.size() - MAX_SUGGESTIONS)));
    }

    private Comparator<AnchorClient> suggestionComparator(String needle) {
        return Comparator
                .comparing((AnchorClient anchor) -> !anchor.name().toLowerCase(Locale.ROOT).startsWith(needle))
                .thenComparing(AnchorClient::name, String.CASE_INSENSITIVE_ORDER);
    }

    private String dimensionShortName(ResourceKey<Level> dimension) {
        Identifier id = dimension.identifier();
        if (id.equals(Identifier.fromNamespaceAndPath("minecraft", "overworld"))) return "overworld";
        if (id.equals(Identifier.fromNamespaceAndPath("minecraft", "the_nether"))) return "nether";
        if (id.equals(Identifier.fromNamespaceAndPath("minecraft", "the_end"))) return "end";
        return id.toString();
    }

    private String suggestionLabel(AnchorClient anchor) {
        Identifier dimensionId = PortSignFormat.parseDimensionId(messages[3]);
        if (dimensionId != null) return anchor.name();
        return anchor.name() + " (" + anchor.dimension().identifier() + ")";
    }

    private void acceptSuggestion(AnchorClient anchor) {
        signportSetMessage(anchor.name());
        // Also fill in the dimension on line 3 when not already specified
        if (PortSignFormat.parseDimensionId(messages[3]) == null) {
            int savedLine = line;
            try {
                line = 3;
                signportSetMessage(dimensionShortName(anchor.dimension()));
            } finally {
                line = savedLine;
            }
        }
        signportDismissed = true;
        signportSuggestions = List.of();
    }

    private void createTemplateWidgets() {
        AbstractSignEditScreen screen = (AbstractSignEditScreen) (Object) this;
        signportTemplateLeft = (screen.width - TEMPLATE_WIDTH) / 2;
        signportTemplateTop = Math.max(28, (screen.height - TEMPLATE_HEIGHT) / 2);
        int fieldX = signportTemplateLeft + 40;
        signportTemplateTargetField = signportAddRenderableWidget(new EditBox(screen.getFont(), fieldX, signportTemplateTop + 18,
                TEMPLATE_FIELD_WIDTH, TEMPLATE_FIELD_HEIGHT, Component.literal("Target anchor")));
        signportTemplateTargetField.setMaxLength(64);
        signportTemplateTargetField.setHint(Component.literal("Anchor name (line 3)"));
        signportTemplateTargetField.setResponder(ignored -> {
            rebuildTemplateDimensions();
            refreshTemplateSuggestions();
        });
        signportTemplateDimensionButton = signportAddRenderableWidget(Button.builder(Component.literal("Dimension (line 4, optional)"),
                        button -> signportTemplateDimensionOpen = !signportTemplateDimensionOpen)
                .bounds(fieldX, signportTemplateTop + 38, TEMPLATE_FIELD_WIDTH, TEMPLATE_FIELD_HEIGHT)
                .build());
        signportTemplateLabelField = signportAddRenderableWidget(new EditBox(screen.getFont(), fieldX, signportTemplateTop + 58,
                TEMPLATE_FIELD_WIDTH, TEMPLATE_FIELD_HEIGHT, Component.literal("Decoration line")));
        signportTemplateLabelField.setMaxLength(64);
        signportTemplateLabelField.setHint(Component.literal("Optional label (line 1)"));
        signportTemplateApplyButton = signportAddRenderableWidget(Button.builder(Component.literal("Apply"),
                        button -> applyTemplateDialog())
                .bounds(signportTemplateLeft + TEMPLATE_WIDTH - 132, signportTemplateTop + TEMPLATE_HEIGHT - 28, 58, 20)
                .build());
        signportTemplateCancelButton = signportAddRenderableWidget(Button.builder(Component.literal("Cancel"),
                        button -> closeTemplateDialog())
                .bounds(signportTemplateLeft + TEMPLATE_WIDTH - 68, signportTemplateTop + TEMPLATE_HEIGHT - 28, 58, 20)
                .build());
    }

    private void openTemplateDialog() {
        if (!canShowTemplateButton()) return;
        signportTemplateOpen = true;
        signportTemplateDimensionOpen = false;
        signportDismissed = true;
        signportSuggestions = List.of();
        signportTemplateTargetField.setValue(PortSignFormat.normalizeLine(messages[2]));
        signportTemplateLabelField.setValue(PortSignFormat.normalizeLine(messages[0]));
        rebuildTemplateDimensions();
        Identifier dimensionId = PortSignFormat.parseDimensionId(messages[3]);
        if (dimensionId != null) {
            selectTemplateDimension(ResourceKey.create(Registries.DIMENSION, dimensionId));
        }
        refreshTemplateSuggestions();
        updateTemplateWidgetVisibility();
        focusTemplateField(signportTemplateTargetField);
    }

    private void closeTemplateDialog() {
        signportTemplateOpen = false;
        signportTemplateDimensionOpen = false;
        signportTemplateSuggestions = List.of();
        updateTemplateWidgetVisibility();
    }

    private void applyTemplateDialog() {
        String anchorName = PortSignFormat.normalizeLine(signportTemplateTargetField.getValue());
        if (anchorName.isBlank()) return;

        int previousLine = line;
        List<String> templateLines = List.of(
                signportTemplateLabelField.getValue(),
                "[sp]",
                anchorName,
                selectedTemplateDimension().map(option -> dimensionShortName(option.dimension())).orElse(""));
        for (int i = 0; i < templateLines.size(); i++) {
            line = i;
            signportSetMessage(templateLines.get(i));
        }
        line = previousLine;
        closeTemplateDialog();
    }

    private boolean handleTemplateKeys(KeyEvent event) {
        if (event.key() == 256) {
            closeTemplateDialog();
            return true;
        }
        if (!signportTemplateTargetField.isFocused()) return false;
        refreshTemplateSuggestions();
        if (signportTemplateSuggestions.isEmpty()) return false;

        switch (event.key()) {
            case 264 -> {
                signportTemplateSelectedSuggestion = (signportTemplateSelectedSuggestion + 1) % signportTemplateSuggestions.size();
                if (signportTemplateSelectedSuggestion == 0) {
                    signportTemplateScrollOffset = 0;
                } else if (signportTemplateSelectedSuggestion >= signportTemplateScrollOffset + MAX_TEMPLATE_SUGGESTIONS) {
                    signportTemplateScrollOffset = signportTemplateSelectedSuggestion - MAX_TEMPLATE_SUGGESTIONS + 1;
                }
                return true;
            }
            case 265 -> {
                signportTemplateSelectedSuggestion = (signportTemplateSelectedSuggestion + signportTemplateSuggestions.size() - 1)
                        % signportTemplateSuggestions.size();
                if (signportTemplateSelectedSuggestion == signportTemplateSuggestions.size() - 1) {
                    signportTemplateScrollOffset = Math.max(0, signportTemplateSuggestions.size() - MAX_TEMPLATE_SUGGESTIONS);
                } else if (signportTemplateSelectedSuggestion < signportTemplateScrollOffset) {
                    signportTemplateScrollOffset = signportTemplateSelectedSuggestion;
                }
                return true;
            }
            case 258, 257 -> {
                acceptTemplateSuggestion(signportTemplateSuggestions.get(signportTemplateSelectedSuggestion));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleTemplateMouseClick(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int visibleTemplateCount = Math.min(MAX_TEMPLATE_SUGGESTIONS, signportTemplateSuggestions.size());
        if (mouseX >= signportTemplateSuggestionX && mouseX < signportTemplateSuggestionX + SUGGESTION_WIDTH
                && mouseY >= signportTemplateSuggestionY
                && mouseY < signportTemplateSuggestionY + visibleTemplateCount * SUGGESTION_ROW_HEIGHT) {
            int index = (int) ((mouseY - signportTemplateSuggestionY) / SUGGESTION_ROW_HEIGHT) + signportTemplateScrollOffset;
            if (index >= 0 && index < signportTemplateSuggestions.size()) {
                acceptTemplateSuggestion(signportTemplateSuggestions.get(index));
                return true;
            }
        }
        if (signportTemplateDimensionOpen
                && mouseX >= signportTemplateDimensionX
                && mouseX < signportTemplateDimensionX + TEMPLATE_FIELD_WIDTH
                && mouseY >= signportTemplateDimensionY
                && mouseY < signportTemplateDimensionY + signportTemplateDimensionOptions.size() * TEMPLATE_ROW_HEIGHT) {
            int index = (int) ((mouseY - signportTemplateDimensionY) / TEMPLATE_ROW_HEIGHT);
            if (index >= 0 && index < signportTemplateDimensionOptions.size()) {
                signportTemplateSelectedDimension = index;
                signportTemplateDimensionOpen = false;
                updateDimensionButtonLabel();
                return true;
            }
        }
        return false;
    }

    private void refreshTemplateSuggestions() {
        if (!signportTemplateOpen || SignPortClientState.anchors().isEmpty()) {
            signportTemplateSuggestions = List.of();
            signportTemplateSelectedSuggestion = 0;
            signportTemplateScrollOffset = 0;
            return;
        }
        String needle = signportTemplateTargetField.getValue().toLowerCase(Locale.ROOT);
        signportTemplateSuggestions = SignPortClientState.anchors().stream()
                .filter(anchor -> needle.isBlank() || anchor.name().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(suggestionComparator(needle))
                .toList();
        if (signportTemplateSelectedSuggestion >= signportTemplateSuggestions.size()) {
            signportTemplateSelectedSuggestion = 0;
            signportTemplateScrollOffset = 0;
        }
        signportTemplateScrollOffset = Math.max(0, Math.min(signportTemplateScrollOffset,
                Math.max(0, signportTemplateSuggestions.size() - MAX_TEMPLATE_SUGGESTIONS)));
        signportTemplateApplyButton.active = !PortSignFormat.normalizeLine(signportTemplateTargetField.getValue()).isBlank();
    }

    private void rebuildTemplateDimensions() {
        String target = PortSignFormat.normalizeLine(signportTemplateTargetField.getValue());
        String needle = target.toLowerCase(Locale.ROOT);
        ResourceKey<Level> previous = selectedTemplateDimension().map(DimensionOption::dimension).orElse(null);
        Map<ResourceKey<Level>, DimensionOption> options = new LinkedHashMap<>();
        List<AnchorClient> exactMatches = new ArrayList<>();
        SignPortClientState.anchors().stream()
                .filter(anchor -> needle.isBlank() || anchor.name().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(anchor -> anchor.dimension().identifier().toString()))
                .forEach(anchor -> {
                    options.putIfAbsent(anchor.dimension(), new DimensionOption(anchor.dimension()));
                    if (!target.isBlank() && anchor.name().equalsIgnoreCase(target)) {
                        exactMatches.add(anchor);
                    }
                });

        List<DimensionOption> rebuilt = new ArrayList<>();
        rebuilt.add(DimensionOption.DEFAULT);
        rebuilt.addAll(options.values());
        signportTemplateDimensionOptions = rebuilt;
        signportTemplateSelectedDimension = 0;

        if (exactMatches.size() == 1) {
            selectTemplateDimension(exactMatches.getFirst().dimension());
        } else if (previous != null) {
            selectTemplateDimension(previous);
        }
        updateDimensionButtonLabel();
    }

    private void selectTemplateDimension(ResourceKey<Level> dimension) {
        for (int i = 0; i < signportTemplateDimensionOptions.size(); i++) {
            DimensionOption option = signportTemplateDimensionOptions.get(i);
            if (!option.isDefault() && option.dimension().equals(dimension)) {
                signportTemplateSelectedDimension = i;
                return;
            }
        }
    }

    private java.util.Optional<DimensionOption> selectedTemplateDimension() {
        if (signportTemplateSelectedDimension < 0 || signportTemplateSelectedDimension >= signportTemplateDimensionOptions.size()) {
            return java.util.Optional.empty();
        }
        DimensionOption option = signportTemplateDimensionOptions.get(signportTemplateSelectedDimension);
        return option.isDefault() ? java.util.Optional.empty() : java.util.Optional.of(option);
    }

    private void acceptTemplateSuggestion(AnchorClient anchor) {
        signportTemplateTargetField.setValue(anchor.name());
        rebuildTemplateDimensions();
        selectTemplateDimension(anchor.dimension());
        updateDimensionButtonLabel();
        signportTemplateSuggestions = List.of();
    }

    private void renderTemplateDialog(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        refreshTemplateSuggestions();
        graphics.nextStratum();
        graphics.fill(0, 0, ((AbstractSignEditScreen) (Object) this).width, ((AbstractSignEditScreen) (Object) this).height, 0x88000000);
        graphics.fill(signportTemplateLeft, signportTemplateTop, signportTemplateLeft + TEMPLATE_WIDTH,
                signportTemplateTop + TEMPLATE_HEIGHT, 0xEE101010);
        graphics.outline(signportTemplateLeft, signportTemplateTop, TEMPLATE_WIDTH, TEMPLATE_HEIGHT, 0xFF6F8CAF);
        graphics.centeredText(net.minecraft.client.Minecraft.getInstance().font, Component.literal("SignPort Template"),
                signportTemplateLeft + TEMPLATE_WIDTH / 2, signportTemplateTop + 10, 0xFFFFFFFF);
        renderTemplateControls(graphics, mouseX, mouseY);
        renderTemplateExample(graphics);
        renderTemplateHelpTooltip(graphics, mouseX, mouseY);

        renderTemplateDimensionDropdown(graphics, mouseX, mouseY);
        renderTemplateSuggestionPopup(graphics, mouseX, mouseY);
    }

    private void renderTemplateControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        signportTemplateTargetField.extractRenderState(graphics, mouseX, mouseY, 0);
        signportTemplateDimensionButton.extractRenderState(graphics, mouseX, mouseY, 0);
        signportTemplateLabelField.extractRenderState(graphics, mouseX, mouseY, 0);
        signportTemplateApplyButton.extractRenderState(graphics, mouseX, mouseY, 0);
        signportTemplateCancelButton.extractRenderState(graphics, mouseX, mouseY, 0);
    }

    private void renderTemplateExample(GuiGraphicsExtractor graphics) {
        int x = signportTemplateLeft + 40;
        int y = signportTemplateTop + 78;
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, "Line 1: Any label (ignored)", x, y, 0xFFBFC9D6);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, "Line 2: [sp]", x, y + 12, 0xFFBFC9D6);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, "Line 3: Anchor name", x, y + 24, 0xFFBFC9D6);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, "Line 4: Dimension or blank", x, y + 36, 0xFFBFC9D6);
        renderTemplateSignPreview(graphics);
    }

    private void renderTemplateSignPreview(GuiGraphicsExtractor graphics) {
        int x = signportTemplateLeft + 40;
        int y = signportTemplateTop + 126;
        int signTextColor = 0xFFFFF4C8;
        graphics.fill(x + 102, y + 54, x + 118, y + 62, 0xFF6F4725);
        graphics.fill(x, y, x + 220, y + 54, 0xFF6F4725);
        graphics.fill(x + 4, y + 4, x + 216, y + 50, 0xFF8A582D);
        graphics.fill(x + 10, y + 8, x + 210, y + 46, 0xFF9B6330);
        graphics.outline(x, y, 220, 54, 0xFF4F321B);
        drawCenteredSignText(graphics, "Home", x + 110, y + 6, signTextColor);
        drawCenteredSignText(graphics, "[signport]", x + 110, y + 18, signTextColor);
        drawCenteredSignText(graphics, "spawn", x + 110, y + 30, signTextColor);
        drawCenteredSignText(graphics, "overworld", x + 110, y + 42, signTextColor);
    }

    private void drawCenteredSignText(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.text(font, text, centerX - font.width(text) / 2, y, color);
    }

    private void renderTemplateHelpTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hoverTemplateExampleLine(mouseX, mouseY, 1)) {
            graphics.setComponentTooltipForNextFrame(net.minecraft.client.Minecraft.getInstance().font, List.of(
                    Component.literal("Line 1 is arbitrary display text."),
                    Component.literal("It does not affect portal creation.")
            ), mouseX, mouseY);
        } else if (hoverTemplateExampleLine(mouseX, mouseY, 2)) {
            graphics.setComponentTooltipForNextFrame(net.minecraft.client.Minecraft.getInstance().font, List.of(
                    Component.literal("Line 2 must be [sp] or [signport]."),
                    Component.literal("This marks the sign as a SignPort portal.")
            ), mouseX, mouseY);
        } else if (hoverTemplateExampleLine(mouseX, mouseY, 3)) {
            graphics.setComponentTooltipForNextFrame(net.minecraft.client.Minecraft.getInstance().font, List.of(
                    Component.literal("Target is the anchor name for line 3."),
                    Component.literal("Choose an existing anchor from suggestions.")
            ), mouseX, mouseY);
        } else if (hoverTemplateExampleLine(mouseX, mouseY, 4)) {
            graphics.setComponentTooltipForNextFrame(net.minecraft.client.Minecraft.getInstance().font, List.of(
                    Component.literal("Enter a dimension when the anchor is in a different dimension."),
                    Component.literal("Leave it blank for the current dimension.")
            ), mouseX, mouseY);
        }
    }

    private void focusTemplateField(EditBox field) {
        signportSetInitialFocus(field);
        signportTemplateTargetField.setFocused(field == signportTemplateTargetField);
        signportTemplateLabelField.setFocused(field == signportTemplateLabelField);
    }

    private boolean hoverTemplateExampleLine(int mouseX, int mouseY, int lineNumber) {
        int x = signportTemplateLeft + 40;
        int y = signportTemplateTop + 78 - 2 + (lineNumber - 1) * 12;
        return mouseX >= x && mouseX < x + 210 && mouseY >= y && mouseY < y + 12;
    }

    private void renderTemplateSuggestionPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (signportTemplateSuggestions.isEmpty() || !signportTemplateTargetField.isFocused()) return;

        int visibleTemplateCount = Math.min(MAX_TEMPLATE_SUGGESTIONS, signportTemplateSuggestions.size());
        signportTemplateSuggestionX = signportTemplateTargetField.getX();
        signportTemplateSuggestionY = signportTemplateTargetField.getY() + signportTemplateTargetField.getHeight() + 2;
        graphics.fill(signportTemplateSuggestionX - 2, signportTemplateSuggestionY - 2,
                signportTemplateSuggestionX + SUGGESTION_WIDTH + 2,
                signportTemplateSuggestionY + visibleTemplateCount * SUGGESTION_ROW_HEIGHT + 2,
                0xF0101010);
        graphics.outline(signportTemplateSuggestionX - 2, signportTemplateSuggestionY - 2, SUGGESTION_WIDTH + 4,
                visibleTemplateCount * SUGGESTION_ROW_HEIGHT + 4, 0xFF6F8CAF);
        for (int i = 0; i < visibleTemplateCount; i++) {
            int idx = signportTemplateScrollOffset + i;
            AnchorClient anchor = signportTemplateSuggestions.get(idx);
            int y = signportTemplateSuggestionY + i * SUGGESTION_ROW_HEIGHT;
            if (idx == signportTemplateSelectedSuggestion || (mouseX >= signportTemplateSuggestionX
                    && mouseX < signportTemplateSuggestionX + SUGGESTION_WIDTH
                    && mouseY >= y && mouseY < y + SUGGESTION_ROW_HEIGHT)) {
                graphics.fill(signportTemplateSuggestionX, y, signportTemplateSuggestionX + SUGGESTION_WIDTH,
                        y + SUGGESTION_ROW_HEIGHT, 0xFF31465E);
            }
            graphics.text(net.minecraft.client.Minecraft.getInstance().font,
                    anchor.name() + " (" + anchor.dimension().identifier() + ")",
                    signportTemplateSuggestionX + 4, y + 3, 0xFFFFFFFF);
        }
    }

    private void renderTemplateDimensionDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!signportTemplateDimensionOpen) return;

        signportTemplateDimensionX = signportTemplateDimensionButton.getX();
        signportTemplateDimensionY = signportTemplateDimensionButton.getY() + signportTemplateDimensionButton.getHeight() + 2;
        graphics.fill(signportTemplateDimensionX - 2, signportTemplateDimensionY - 2,
                signportTemplateDimensionX + TEMPLATE_FIELD_WIDTH + 2,
                signportTemplateDimensionY + signportTemplateDimensionOptions.size() * TEMPLATE_ROW_HEIGHT + 2,
                0xF0101010);
        graphics.outline(signportTemplateDimensionX - 2, signportTemplateDimensionY - 2, TEMPLATE_FIELD_WIDTH + 4,
                signportTemplateDimensionOptions.size() * TEMPLATE_ROW_HEIGHT + 4, 0xFF6F8CAF);
        for (int i = 0; i < signportTemplateDimensionOptions.size(); i++) {
            int y = signportTemplateDimensionY + i * TEMPLATE_ROW_HEIGHT;
            if (i == signportTemplateSelectedDimension || (mouseX >= signportTemplateDimensionX
                    && mouseX < signportTemplateDimensionX + TEMPLATE_FIELD_WIDTH
                    && mouseY >= y && mouseY < y + TEMPLATE_ROW_HEIGHT)) {
                graphics.fill(signportTemplateDimensionX, y, signportTemplateDimensionX + TEMPLATE_FIELD_WIDTH,
                        y + TEMPLATE_ROW_HEIGHT, 0xFF31465E);
            }
            graphics.text(net.minecraft.client.Minecraft.getInstance().font, signportTemplateDimensionOptions.get(i).label(),
                    signportTemplateDimensionX + 4, y + 3, 0xFFFFFFFF);
        }
    }

    private void updateTemplateButtonVisibility() {
        if (signportTemplateButton != null) {
            signportTemplateButton.visible = canShowTemplateButton();
            signportTemplateButton.active = canShowTemplateButton();
        }
        if (!canShowTemplateButton() && signportTemplateOpen) {
            closeTemplateDialog();
        }
    }

    private boolean canShowTemplateButton() {
        return SignPortClientConfig.get().signTemplateButtonEnabled
                && SignPortClientState.permissions().canCreatePortSign();
    }

    private void updateTemplateWidgetVisibility() {
        if (signportTemplateTargetField == null) return;
        signportTemplateTargetField.visible = signportTemplateOpen;
        signportTemplateTargetField.active = signportTemplateOpen;
        signportTemplateLabelField.visible = signportTemplateOpen;
        signportTemplateLabelField.active = signportTemplateOpen;
        signportTemplateDimensionButton.visible = signportTemplateOpen;
        signportTemplateDimensionButton.active = signportTemplateOpen && signportTemplateDimensionOptions.size() > 1;
        signportTemplateApplyButton.visible = signportTemplateOpen;
        signportTemplateApplyButton.active = signportTemplateOpen
                && !PortSignFormat.normalizeLine(signportTemplateTargetField.getValue()).isBlank();
        signportTemplateCancelButton.visible = signportTemplateOpen;
        signportTemplateCancelButton.active = signportTemplateOpen;
        updateDimensionButtonLabel();
    }

    private void updateDimensionButtonLabel() {
        if (signportTemplateDimensionButton == null || signportTemplateDimensionOptions.isEmpty()) return;
        signportTemplateDimensionButton.setMessage(Component.literal("Dimension: "
                + signportTemplateDimensionOptions.get(signportTemplateSelectedDimension).label()));
    }

    private record DimensionOption(ResourceKey<Level> dimension) {
        private static final DimensionOption DEFAULT = new DimensionOption(null);

        boolean isDefault() {
            return dimension == null;
        }

        String label() {
            return isDefault() ? "(use current dimension default)" : dimension.identifier().toString();
        }
    }

    private final class SuggestionClickTarget extends AbstractWidget {
        private SuggestionClickTarget() {
            super(0, 0, 0, 0, net.minecraft.network.chat.Component.empty());
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return handleSuggestionMouseClick(event);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (signportTemplateOpen) {
                int visibleTemplateCount = Math.min(MAX_TEMPLATE_SUGGESTIONS, signportTemplateSuggestions.size());
                return (mouseX >= signportTemplateSuggestionX
                        && mouseX < signportTemplateSuggestionX + SUGGESTION_WIDTH
                        && mouseY >= signportTemplateSuggestionY
                        && mouseY < signportTemplateSuggestionY + visibleTemplateCount * SUGGESTION_ROW_HEIGHT)
                        || (signportTemplateDimensionOpen
                        && mouseX >= signportTemplateDimensionX
                        && mouseX < signportTemplateDimensionX + TEMPLATE_FIELD_WIDTH
                        && mouseY >= signportTemplateDimensionY
                        && mouseY < signportTemplateDimensionY + signportTemplateDimensionOptions.size() * TEMPLATE_ROW_HEIGHT);
            }
            refreshSuggestions();
            int visibleCount = Math.min(MAX_SUGGESTIONS, signportSuggestions.size());
            return !signportSuggestions.isEmpty()
                    && mouseX >= signportPopupX
                    && mouseX < signportPopupX + SUGGESTION_WIDTH
                    && mouseY >= signportPopupY
                    && mouseY < signportPopupY + visibleCount * SUGGESTION_ROW_HEIGHT;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
