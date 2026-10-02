package fr.poubone.att2.client.renderer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * ATT2 {@code custom_data.Rarity} values and the HUD / loot-beam look that goes with each one.
 */
public enum ItemRarity {
    COM("com", 0x9CA3AF, false, false, false, 0.90f),
    UNC("unc", 0x4ADE80, false, false, false, 1.00f),
    RAR("rar", 0x60A5FA, true, false, false, 1.08f),
    EPI("epi", 0xC084FC, true, false, false, 1.16f),
    EPI_ESC("epi_esc", 0xE879F9, true, false, false, 1.20f),
    EPI_SET("epi_set", 0xFB923C, true, false, false, 1.22f),
    LEG("leg", 0xFBBF24, true, true, false, 1.32f),
    LEG_ARMSET("leg_armset", 0xF59E0B, true, true, false, 1.34f),
    ULT("ult", 0x55FF55, true, true, false, 1.45f),
    MYT("myt", 0xF5F3FF, true, true, true, 1.70f),
    SPE("spe", 0xF472B6, true, false, false, 1.18f),
    QUE("que", 0x22D3EE, false, false, false, 1.00f),
    MISC("misc", 0xD1D5DB, false, false, false, 0.90f),
    UNK("unk", 0xE5E7EB, false, false, false, 0.95f),
    CUR("cur", 0xEF4444, false, false, false, 0.95f);

    public final String id;
    public final int rgb;
    public final boolean shimmer;
    public final boolean helix;
    public final boolean sparks;
    public final float pitch;

    ItemRarity(String id, int rgb, boolean shimmer, boolean helix, boolean sparks, float pitch) {
        this.id = id;
        this.rgb = rgb;
        this.shimmer = shimmer;
        this.helix = helix;
        this.sparks = sparks;
        this.pitch = pitch;
    }

    public static ItemRarity fromId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (ItemRarity rarity : values()) {
            if (rarity.id.equals(id)) return rarity;
        }
        return null;
    }

    public static ItemRarity fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return null;
        return data.copyTag().getString("Rarity").map(ItemRarity::fromId).orElse(null);
    }

    public int argb(int alpha) {
        return (alpha << 24) | rgb;
    }
}
