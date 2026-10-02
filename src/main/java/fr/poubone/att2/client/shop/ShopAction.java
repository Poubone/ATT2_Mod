package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;

/**
 * A shop line that is not an item purchase: reset, mending, tool prices, etc.
 */
public record ShopAction(int trigger, Component label, Component tip, Kind kind) {
    public enum Kind {
        RESET,
        BACK,
        MENDING_PRICE,
        MENDING_REPAIR,
        MENDING_TOOLS,
        MENDING_SLOT,
        OTHER
    }

    public boolean isMending() {
        return kind == Kind.MENDING_PRICE || kind == Kind.MENDING_REPAIR
                || kind == Kind.MENDING_TOOLS || kind == Kind.MENDING_SLOT;
    }
}
