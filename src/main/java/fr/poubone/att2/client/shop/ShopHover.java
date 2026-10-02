package fr.poubone.att2.client.shop;

/** Click leaves keyboard focus on tabs and page buttons; tooltips must follow the cursor only. */
public final class ShopHover {
    public enum Kind { SLOT, ACTION }

    public record Widget(String id, Kind kind, boolean hovered, boolean focused) {
    }

    private ShopHover() {
    }

    public static boolean showsTooltip(boolean hovered, boolean focused) {
        return hovered;
    }

    public static String pick(Widget... widgets) {
        String slot = null;
        String action = null;
        for (Widget widget : widgets) {
            if (!showsTooltip(widget.hovered, widget.focused)) continue;
            if (widget.kind == Kind.SLOT) slot = widget.id;
            else action = widget.id;
        }
        return slot != null ? slot : action;
    }
}
