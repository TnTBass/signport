package tech.endorsed.signport.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignPortConfigScreenLayoutTest {
    @Test
    void statusDotSitsAtTopRightOfContent() {
        SignPortConfigScreenLayout layout = SignPortConfigScreenLayout.create(854, 480);

        assertEquals(8, layout.statusWidth());
        assertEquals(layout.left() + layout.contentWidth() - layout.statusWidth(), layout.statusX());
        assertTrue(layout.statusY() >= layout.titleY());
        assertTrue(layout.titleWidth() < layout.contentWidth());
    }

    @Test
    void layoutKeepsControlsInsideNarrowScreen() {
        SignPortConfigScreenLayout layout = SignPortConfigScreenLayout.create(240, 240);

        assertTrue(layout.left() >= 12);
        assertTrue(layout.contentWidth() <= 240 - 24);
        assertTrue(layout.buttonY() > layout.templateButtonY());
        assertEquals(layout.left() + layout.contentWidth() - layout.buttonWidth(), layout.cancelButtonX());
    }

    @Test
    void layoutDoesNotPlaceControlsAboveTinyScreen() {
        SignPortConfigScreenLayout layout = SignPortConfigScreenLayout.create(160, 80);

        assertTrue(layout.hudHintY() >= 0);
        assertTrue(layout.browserKeybindY() >= 0);
        assertTrue(layout.autocompleteY() >= 0);
        assertTrue(layout.templateButtonY() >= 0);
    }
}
