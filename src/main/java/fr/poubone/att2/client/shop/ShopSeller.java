package fr.poubone.att2.client.shop;

/**
 * Static profile of a map merchant: opening trigger, stall type, and which chrome buttons exist.
 */
public record ShopSeller(int trigger, String npcId, ShopType type, boolean repair, boolean reset, boolean gambling) {
}
