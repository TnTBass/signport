package tech.endorsed.signport.client.gui;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.client.SignPortClientState;
import tech.endorsed.signport.world.AnchorCreation;

final class AnchorCreateValidation {
    private AnchorCreateValidation() {
    }

    static State validate(String name, ResourceKey<Level> currentDimension) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > AnchorCreation.MAX_ANCHOR_NAME_LENGTH || currentDimension == null) {
            return State.RED;
        }
        if (SignPortClientState.find(normalized, currentDimension).isPresent()) {
            return State.RED;
        }
        if (SignPortClientState.findAnyDimension(normalized).isPresent()) {
            return State.ORANGE;
        }
        return State.GREEN;
    }

    enum State {
        GREEN,
        ORANGE,
        RED
    }
}
