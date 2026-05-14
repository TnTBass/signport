package tech.endorsed.signport.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.Registries;
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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    private static final int MAX_SUGGESTIONS = 8;
    private static final int SUGGESTION_WIDTH = 220;
    private static final int SUGGESTION_ROW_HEIGHT = 14;

    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private String[] messages;
    @Shadow private int line;

    private List<AnchorClient> signportSuggestions = List.of();
    private int signportSelectedSuggestion = 0;
    private boolean signportDismissed = false;
    private int signportPopupX;
    private int signportPopupY;

    @Invoker("setMessage")
    protected abstract void signportSetMessage(String message);

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void signportAddSuggestionClickTarget(CallbackInfo ci) {
        addRenderableWidget(new SuggestionClickTarget());
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void signportHandleSuggestionKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        refreshSuggestions();
        if (signportSuggestions.isEmpty()) return;

        switch (event.key()) {
            case 264 -> {
                signportSelectedSuggestion = (signportSelectedSuggestion + 1) % signportSuggestions.size();
                cir.setReturnValue(true);
            }
            case 265 -> {
                signportSelectedSuggestion = (signportSelectedSuggestion + signportSuggestions.size() - 1) % signportSuggestions.size();
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
        if (event.key() != 256 && event.key() != 258 && event.key() != 257 && event.key() != 264 && event.key() != 265) {
            signportDismissed = false;
        }
        refreshSuggestions();
    }

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void signportAfterChar(CallbackInfoReturnable<Boolean> cir) {
        signportDismissed = false;
        refreshSuggestions();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void signportRenderSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        refreshSuggestions();
        if (signportSuggestions.isEmpty()) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        signportPopupX = Math.max(12, Math.min(width - SUGGESTION_WIDTH - 12, width / 2 - SUGGESTION_WIDTH / 2));
        signportPopupY = Math.min(height - 20 - signportSuggestions.size() * SUGGESTION_ROW_HEIGHT, height / 2 + 56);

        graphics.nextStratum();
        graphics.fill(signportPopupX - 2, signportPopupY - 2, signportPopupX + SUGGESTION_WIDTH + 2,
                signportPopupY + signportSuggestions.size() * SUGGESTION_ROW_HEIGHT + 2, 0xDD101010);
        graphics.outline(signportPopupX - 2, signportPopupY - 2, SUGGESTION_WIDTH + 4,
                signportSuggestions.size() * SUGGESTION_ROW_HEIGHT + 4, 0xFF6F8CAF);

        for (int i = 0; i < signportSuggestions.size(); i++) {
            AnchorClient anchor = signportSuggestions.get(i);
            int y = signportPopupY + i * SUGGESTION_ROW_HEIGHT;
            if (i == signportSelectedSuggestion) {
                graphics.fill(signportPopupX, y, signportPopupX + SUGGESTION_WIDTH, y + SUGGESTION_ROW_HEIGHT, 0xFF31465E);
            }
            graphics.text(net.minecraft.client.Minecraft.getInstance().font, suggestionLabel(anchor), signportPopupX + 4, y + 3, 0xFFFFFFFF);
        }
    }

    private boolean handleSuggestionMouseClick(MouseButtonEvent event) {
        refreshSuggestions();
        if (signportSuggestions.isEmpty()) return false;

        double mouseX = event.x();
        double mouseY = event.y();
        if (mouseX < signportPopupX || mouseX >= signportPopupX + SUGGESTION_WIDTH || mouseY < signportPopupY) return false;
        int index = (int) ((mouseY - signportPopupY) / SUGGESTION_ROW_HEIGHT);
        if (index >= 0 && index < signportSuggestions.size()) {
            acceptSuggestion(signportSuggestions.get(index));
            return true;
        }
        return false;
    }

    private void refreshSuggestions() {
        if (!SignPortClientConfig.get().signEditorAutocompleteEnabled || SignPortClientState.anchors().isEmpty()
                || signportDismissed || line != 2 || !PortSignFormat.isPortalMarker(messages[1])) {
            signportSuggestions = List.of();
            signportSelectedSuggestion = 0;
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
                .limit(MAX_SUGGESTIONS)
                .toList();
        if (signportSelectedSuggestion >= signportSuggestions.size()) {
            signportSelectedSuggestion = 0;
        }
    }

    private Comparator<AnchorClient> suggestionComparator(String needle) {
        return Comparator
                .comparing((AnchorClient anchor) -> !anchor.name().toLowerCase(Locale.ROOT).startsWith(needle))
                .thenComparing(AnchorClient::name, String.CASE_INSENSITIVE_ORDER);
    }

    private String suggestionLabel(AnchorClient anchor) {
        Identifier dimensionId = PortSignFormat.parseDimensionId(messages[3]);
        if (dimensionId != null) return anchor.name();
        return anchor.name() + " (" + anchor.dimension().identifier() + ")";
    }

    private void acceptSuggestion(AnchorClient anchor) {
        signportSetMessage(anchor.name());
        signportDismissed = true;
        signportSuggestions = List.of();
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
            refreshSuggestions();
            return !signportSuggestions.isEmpty()
                    && mouseX >= signportPopupX
                    && mouseX < signportPopupX + SUGGESTION_WIDTH
                    && mouseY >= signportPopupY
                    && mouseY < signportPopupY + signportSuggestions.size() * SUGGESTION_ROW_HEIGHT;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
