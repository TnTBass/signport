package tech.endorsed.signport.client;

import net.minecraft.client.gui.screens.Screen;
import tech.endorsed.signport.SignPort;

public final class ScreenNavigation {
    private static Navigator navigator =
            screen -> SignPort.LOGGER.warn("[SignPort] Cannot navigate screens; no client navigation hook is installed.");

    private ScreenNavigation() {
    }

    public static void install(Navigator installedNavigator) {
        navigator = installedNavigator == null ? screen -> {
        } : installedNavigator;
    }

    public static void show(Screen screen) {
        navigator.show(screen);
    }

    public interface Navigator {
        void show(Screen screen);
    }
}
