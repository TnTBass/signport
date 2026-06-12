package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class AnchorCreation {
    public static final int MAX_ANCHOR_NAME_LENGTH = 14;
    public static final String CLEAR_GROUP_SENTINEL = "-";

    private AnchorCreation() {
    }

    public static Result create(
            AnchorState state,
            String name,
            BlockPos pos,
            ResourceKey<Level> dimension,
            String group
    ) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > MAX_ANCHOR_NAME_LENGTH) {
            return Result.failure(Error.INVALID_NAME);
        }
        if (state.findAnchor(normalizedName, dimension).isPresent()) {
            return Result.failure(Error.NAME_CLASH);
        }
        for (Anchor anchor : state.getAnchorsForDimension(dimension)) {
            if (anchor.pos.equals(pos)) {
                return Result.failure(Error.POSITION_CLASH);
            }
        }

        Anchor anchor = new Anchor(normalizedName, pos, dimension, normalizeGroup(group));
        state.addAnchor(anchor);
        return Result.success(anchor);
    }

    public static String normalizeGroup(String group) {
        if (group == null || group.equals(CLEAR_GROUP_SENTINEL)) return "";
        return group;
    }

    public enum Error {
        NONE(""),
        INVALID_NAME("Invalid anchor name"),
        NAME_CLASH("An anchor with that name already exists in this dimension"),
        POSITION_CLASH("An anchor already exists at this position");

        private final String message;

        Error(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    public record Result(boolean success, Anchor anchor, Error error) {
        public static Result success(Anchor anchor) {
            return new Result(true, anchor, Error.NONE);
        }

        public static Result failure(Error error) {
            return new Result(false, null, error);
        }

        public String errorMessage() {
            return error.message();
        }
    }
}
