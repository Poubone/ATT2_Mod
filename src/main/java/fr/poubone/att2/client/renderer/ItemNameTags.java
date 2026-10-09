package fr.poubone.att2.client.renderer;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * HUD config {@code cacheNameTags}: a dropped item's name tag is built, measured and laid out once, then reused while
 * its name, team and the fonts stay the same. Vanilla does all three from scratch every frame for every tag, and lays
 * each one out twice (once for the pass through walls).
 */
public final class ItemNameTags {
    /** What an item's tag is made from, compared by identity, and the tag built from it. */
    private record Name(Component customName, ItemStack stack, PlayerTeam team, ChatFormatting color, Component prefix,
                        Component suffix, Component tag) {
    }

    /** The width and the laid-out text of one tag. A tag is drawn with two looks (through walls, and normally). */
    private static final class Layout {
        int fontEpoch = -1;
        int width = -1;
        int used;
        Boolean obfuscated;
        final Slot[] slots = new Slot[2];
    }

    private record Slot(Font font, float x, float y, int color, boolean shadow, int background, Font.PreparedText text) {
    }

    private static final Map<ItemEntity, Name> NAMES = new WeakHashMap<>();
    /** Only tags built here: other entities' tags are new objects every frame. */
    private static final Map<Component, Layout> LAYOUTS = new IdentityHashMap<>();
    private static final int SWEEP_EVERY = 512;
    private static int fontEpoch;
    private static int frame;

    private ItemNameTags() {
    }

    /** The tag built for this item before, if what it is made from has not changed. */
    public static Component cached(ItemEntity item) {
        Name name = NAMES.get(item);
        if (name == null) return null;
        PlayerTeam team = item.getTeam();
        if (name.customName != item.getCustomName() || name.stack != item.getItem() || name.team != team) return null;
        if (team != null && (name.color != team.getColor() || name.prefix != team.getPlayerPrefix() || name.suffix != team.getPlayerSuffix())) {
            return null;
        }
        return name.tag;
    }

    public static void remember(ItemEntity item, Component tag) {
        PlayerTeam team = item.getTeam();
        Name old = NAMES.put(item, new Name(item.getCustomName(), item.getItem(), team, team == null ? null : team.getColor(),
                team == null ? null : team.getPlayerPrefix(), team == null ? null : team.getPlayerSuffix(), tag));
        if (old != null) LAYOUTS.remove(old.tag);
        LAYOUTS.put(tag, new Layout());
    }

    /** The width of a tag built here, measured once; -1 for any other text. */
    public static int width(Font font, Component tag) {
        Layout layout = layout(tag);
        if (layout == null) return -1;
        if (layout.width < 0) layout.width = font.width(tag);
        return layout.width;
    }

    /** A tag built here laid out with these settings, reusing the last layout; null for any other text. */
    public static Font.PreparedText prepared(Font font, Component tag, float x, float y, int color, boolean shadow, int background) {
        Layout layout = layout(tag);
        if (layout == null) return null;
        if (layout.obfuscated == null) {
            // Obfuscated characters change every time they are laid out.
            layout.obfuscated = tag.visit((style, text) -> style.isObfuscated() ? Optional.of(Boolean.TRUE) : Optional.empty(),
                    Style.EMPTY).isPresent();
        }
        if (layout.obfuscated) return null;
        Slot[] slots = layout.slots;
        for (Slot slot : slots) {
            if (slot != null && slot.font == font && slot.x == x && slot.y == y && slot.color == color && slot.shadow == shadow
                    && slot.background == background) {
                return slot.text;
            }
        }
        Font.PreparedText text = font.prepareText(tag.getVisualOrderText(), x, y, color, shadow, false, background);
        int free = slots[0] == null ? 0 : slots[1] == null ? 1 : 0;
        slots[free] = new Slot(font, x, y, color, shadow, background, text);
        return text;
    }

    private static Layout layout(Component tag) {
        if (!HUDConfig.get().cacheNameTags) return null;
        Layout layout = LAYOUTS.get(tag);
        if (layout == null) return null;
        if (layout.fontEpoch != fontEpoch) {
            layout.fontEpoch = fontEpoch;
            layout.width = -1;
            layout.slots[0] = null;
            layout.slots[1] = null;
        }
        layout.used = frame;
        return layout;
    }

    /** Called once per name tag pass; forgets layouts that have not been drawn for a while. */
    public static void nextFrame() {
        if (++frame % SWEEP_EVERY == 0) {
            LAYOUTS.values().removeIf(layout -> frame - layout.used > SWEEP_EVERY);
        }
    }

    /** Glyphs were rebuilt (resource reload, font options): every layout points at old glyphs. */
    public static void fontsChanged() {
        fontEpoch++;
    }
}
