package fr.poubone.att2.client.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashSet;
import java.util.Set;

/** Which spell offers the player already has, for the Dahal stall's "hide owned spells" filter. */
final class ShopOwnedSpells {
    /** Enhancement lines (one per spell) use these triggers; buying one needs the spell, so carrying it does not count. */
    static final int ENHANCEMENT_FIRST = 424, ENHANCEMENT_LAST = 432;

    private ShopOwnedSpells() {
    }

    static boolean isEnhancement(int trigger) {
        return trigger >= ENHANCEMENT_FIRST && trigger <= ENHANCEMENT_LAST;
    }

    /** Spell ids of items in the inventory, armor and offhand, as the map's own "already have" check reads them. */
    static Set<Integer> carried(Player player) {
        Set<Integer> spells = new HashSet<>();
        if (player == null) return spells;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            int spell = spellOf(inventory.getItem(slot));
            if (spell > 0) spells.add(spell);
        }
        return spells;
    }

    /**
     * Marked owned by the map (learned, or an enhancement maxed out), or a spell the player is carrying.
     */
    static boolean isOwned(int trigger, int spellId, boolean markedOwned, Set<Integer> carried) {
        if (markedOwned) return true;
        return spellId > 0 && !isEnhancement(trigger) && carried.contains(spellId);
    }

    private static int spellOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return 0;
        return data.copyTag().getIntOr("Spell", 0);
    }
}
