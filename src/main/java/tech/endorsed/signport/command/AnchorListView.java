package tech.endorsed.signport.command;

import tech.endorsed.signport.world.Anchor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure helpers for filtering, paginating, and clamping the chat-based
 * /sp anchor list view. Kept free of Minecraft types so they can be unit-tested.
 */
public final class AnchorListView {
    private AnchorListView() {
    }

    /** Case-insensitive substring filter on anchor name. Empty/null filter returns the input unchanged. */
    public static List<Anchor> filter(List<Anchor> anchors, String filter) {
        if (filter == null || filter.isEmpty()) return anchors;
        String needle = filter.toLowerCase(Locale.ROOT);
        return anchors.stream()
                .filter(a -> a.name.toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /** Sorts anchors by group first, then anchor name; ungrouped anchors sort last. */
    public static List<Anchor> sortByGroupThenName(List<Anchor> anchors) {
        return anchors.stream()
                .sorted(Comparator
                        .comparing(AnchorListView::groupSortKey, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(a -> a.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String groupSortKey(Anchor anchor) {
        if (anchor.group == null || anchor.group.isEmpty()) return "\uffff";
        return anchor.group;
    }

    /** Total page count for {@code total} items at {@code pageSize} per page; minimum 1. */
    public static int totalPages(int total, int pageSize) {
        if (pageSize < 1) pageSize = 1;
        if (total <= 0) return 1;
        return (total + pageSize - 1) / pageSize;
    }

    /** Clamps a 1-based page number into {@code [1, totalPages]}. */
    public static int clampPage(int requested, int totalPages) {
        if (totalPages < 1) totalPages = 1;
        if (requested < 1) return 1;
        if (requested > totalPages) return totalPages;
        return requested;
    }

    /** 1-based page slice. Returns empty when out of range, but callers should clamp first. */
    public static <T> List<T> slice(List<T> items, int page, int pageSize) {
        if (pageSize < 1) pageSize = 1;
        if (page < 1) page = 1;
        int from = (page - 1) * pageSize;
        if (from >= items.size()) return List.of();
        int to = Math.min(items.size(), from + pageSize);
        return items.subList(from, to);
    }
}
