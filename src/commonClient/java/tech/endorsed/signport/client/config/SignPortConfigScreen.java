package tech.endorsed.signport.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tech.endorsed.signport.client.ScreenNavigation;
import tech.endorsed.signport.internal.modstatus.ModStatusDisplay;
import tech.endorsed.signport.status.SignPortStatus;
import tech.endorsed.signport.status.SignPortStatusDisplay;

import java.util.List;

public final class SignPortConfigScreen extends Screen {
    private final Screen parent;
    private boolean hudHintEnabled;
    private boolean browserKeybindEnabled;
    private boolean signEditorAutocompleteEnabled;
    private boolean signTemplateButtonEnabled;

    public SignPortConfigScreen(Screen parent) {
        super(Component.literal("SignPort"));
        this.parent = parent;
        SignPortClientConfig.Values config = SignPortClientConfig.get();
        this.hudHintEnabled = config.hudHintEnabled;
        this.browserKeybindEnabled = config.browserKeybindEnabled;
        this.signEditorAutocompleteEnabled = config.signEditorAutocompleteEnabled;
        this.signTemplateButtonEnabled = config.signTemplateButtonEnabled;
    }

    @Override
    protected void init() {
        SignPortConfigScreenLayout layout = SignPortConfigScreenLayout.create(this.width, this.height);
        int left = layout.left();
        int rowHeight = layout.rowHeight();

        addRenderableOnly(new StringWidget(left, layout.titleY(), layout.titleWidth(), rowHeight, Component.literal("SignPort"), this.font));
        addRenderableOnly(new StatusDot(layout));

        addRenderableWidget(Checkbox.builder(Component.literal("HUD lookup hint"), this.font)
                .pos(left, layout.hudHintY())
                .selected(hudHintEnabled)
                .onValueChange((checkbox, selected) -> hudHintEnabled = selected)
                .build());
        addRenderableWidget(Checkbox.builder(Component.literal("Enable anchor browser keybind"), this.font)
                .pos(left, layout.browserKeybindY())
                .selected(browserKeybindEnabled)
                .onValueChange((checkbox, selected) -> browserKeybindEnabled = selected)
                .build());
        addRenderableWidget(Checkbox.builder(Component.literal("Sign editor autocomplete"), this.font)
                .pos(left, layout.autocompleteY())
                .selected(signEditorAutocompleteEnabled)
                .onValueChange((checkbox, selected) -> signEditorAutocompleteEnabled = selected)
                .build());
        addRenderableWidget(Checkbox.builder(Component.literal("Sign template button"), this.font)
                .pos(left, layout.templateButtonY())
                .selected(signTemplateButtonEnabled)
                .onValueChange((checkbox, selected) -> signTemplateButtonEnabled = selected)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
                .bounds(left, layout.buttonY(), layout.buttonWidth(), rowHeight)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(layout.cancelButtonX(), layout.buttonY(), layout.buttonWidth(), rowHeight)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            ScreenNavigation.show(parent);
        }
    }

    private void saveAndClose() {
        SignPortClientConfig.setHudHintEnabled(hudHintEnabled);
        SignPortClientConfig.setBrowserKeybindEnabled(browserKeybindEnabled);
        SignPortClientConfig.setSignEditorAutocompleteEnabled(signEditorAutocompleteEnabled);
        SignPortClientConfig.setSignTemplateButtonEnabled(signTemplateButtonEnabled);
        SignPortClientConfig.save();
        onClose();
    }

    private static List<Component> tooltipLines(ModStatusDisplay display) {
        return SignPortStatusDisplay.tooltipText(display).stream()
                .<Component>map(Component::literal)
                .toList();
    }

    private final class StatusDot implements Renderable {
        private final int x;
        private final int y;
        private final int size;

        private StatusDot(SignPortConfigScreenLayout layout) {
            this.x = layout.statusX();
            this.y = layout.statusY();
            this.size = layout.statusWidth();
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            ModStatusDisplay display = SignPortStatus.clientState().display();
            graphics.fill(x, y, x + size, y + size, SignPortStatusDisplay.STATUS_SQUARE_BORDER_COLOR);
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, SignPortStatusDisplay.toneColor(display.tone()));
            if (isHovered(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(font, tooltipLines(display), mouseX, mouseY);
            }
        }

        private boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        }
    }
}
