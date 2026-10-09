package fr.poubone.att2.client.renderer;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Dropped items lying in the same block, with the same rarity, form a pile. With HUD config {@code lootPiles}
 * on, one item per pile gets the beam and the map's dust, so a mixed pile still shows one beam per rarity it
 * holds. The item with the lowest entity id stands for the pile, which keeps it steady as the camera moves.
 */
public final class ItemPiles {
    /** Rarity id per stack ({@code ""} for none), so the item NBT is not copied on every lookup. */
    private static final Map<ItemStack, String> RARITY = new WeakHashMap<>();

    private record PileKey(int x, int y, int z, String rarity) {
    }

    private ItemPiles() {
    }

    public static boolean enabled() {
        return HUDConfig.get().lootPiles;
    }

    /** One item per pile among {@code items}. */
    public static Set<ItemEntity> representatives(Collection<ItemEntity> items) {
        return representatives(items, ItemPiles::key, ItemEntity::getId);
    }

    static <T> Set<T> representatives(Collection<T> items, Function<T, Object> key, ToIntFunction<T> id) {
        Map<Object, T> chosen = new HashMap<>();
        for (T item : items) {
            chosen.merge(key.apply(item), item, (a, b) -> id.applyAsInt(a) <= id.applyAsInt(b) ? a : b);
        }
        Set<T> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(chosen.values());
        return result;
    }

    private static Object key(ItemEntity item) {
        return new PileKey(Mth.floor(item.getX()), Mth.floor(item.getY()), Mth.floor(item.getZ()), rarity(item.getItem()));
    }

    private static String rarity(ItemStack stack) {
        return RARITY.computeIfAbsent(stack, s -> {
            ItemRarity rarity = ItemRarity.fromStack(s);
            return rarity == null ? "" : rarity.id;
        });
    }
}
