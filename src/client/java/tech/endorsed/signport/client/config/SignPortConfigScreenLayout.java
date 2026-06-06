package tech.endorsed.signport.client.config;

record SignPortConfigScreenLayout(
        int left,
        int titleY,
        int titleWidth,
        int statusX,
        int statusY,
        int statusWidth,
        int hudHintY,
        int browserKeybindY,
        int autocompleteY,
        int templateButtonY,
        int buttonY,
        int contentWidth,
        int buttonWidth,
        int rowHeight
) {
    private static final int CONTENT_WIDTH = 420;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int STATUS_SIZE = 8;
    private static final int MIN_MARGIN = 12;
    private static final int FOOTER_MARGIN = 8;
    private static final int TITLE_STATUS_GAP = 8;

    static SignPortConfigScreenLayout create(int screenWidth, int screenHeight) {
        int contentWidth = Math.min(CONTENT_WIDTH, Math.max(180, screenWidth - MIN_MARGIN * 2));
        int left = Math.max(MIN_MARGIN, (screenWidth - contentWidth) / 2);
        int buttonY = Math.max(screenHeight - ROW_HEIGHT - FOOTER_MARGIN, 0);
        int titleY = screenHeight < 260 ? 10 : 20;
        int rowSpacing = screenHeight < 260 ? 23 : 27;
        int y = titleY + (screenHeight < 260 ? 28 : 36);
        int statusWidth = STATUS_SIZE;
        int statusX = left + contentWidth - statusWidth;
        int titleWidth = Math.max(80, statusX - left - TITLE_STATUS_GAP);
        int statusY = titleY + (ROW_HEIGHT - statusWidth) / 2;

        int lastControlY = Math.max(titleY + ROW_HEIGHT, buttonY - ROW_HEIGHT - rowSpacing);
        int hudHintY = Math.max(0, Math.min(y, lastControlY - rowSpacing * 3));
        y = hudHintY + rowSpacing;
        int browserKeybindY = Math.max(0, Math.min(y, lastControlY - rowSpacing * 2));
        y = browserKeybindY + rowSpacing;
        int autocompleteY = Math.max(0, Math.min(y, lastControlY - rowSpacing));
        y = autocompleteY + rowSpacing;
        int templateButtonY = Math.max(0, Math.min(y, lastControlY));

        return new SignPortConfigScreenLayout(
                left,
                titleY,
                titleWidth,
                statusX,
                statusY,
                statusWidth,
                hudHintY,
                browserKeybindY,
                autocompleteY,
                templateButtonY,
                buttonY,
                contentWidth,
                Math.min(BUTTON_WIDTH, (contentWidth - 8) / 2),
                ROW_HEIGHT
        );
    }

    int cancelButtonX() {
        return left + contentWidth - buttonWidth;
    }
}
