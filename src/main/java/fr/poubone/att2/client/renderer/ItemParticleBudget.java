package fr.poubone.att2.client.renderer;

import fr.poubone.att2.client.hud.HUDConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * The map's rarity dust: every tick it sends a few dust particles above each dropped item within 50 blocks.
 * This recognises that dust (items are indexed by block column once per tick, so each of the thousands of
 * packets a second is a couple of hash lookups) and
 * <ul>
 *   <li>spawns it without block collisions, which it never needs and which dominate particle ticking;</li>
 *   <li>with piles on ({@link ItemPiles}), keeps it on one item per pile and rarity;</li>
 *   <li>with HUD config {@code mapItemParticleLimit} set, keeps it on the nearest items only.</li>
 * </ul>
 */
public final class ItemParticleBudget {
    public enum Verdict { NOT_ITEM_DUST, SHOW, HIDE }

    /**
     * The map spawns each item's dust at the item's own x and z, 0.5 to 1.5 blocks up; in a heap items lie a
     * few tenths of a block apart, so the dust is matched to the closest item, within {@link #SIDE}.
     */
    private static final double ABOVE = 1.8, BELOW = 0.3, SIDE = 0.1;
    private static Long2ObjectOpenHashMap<List<ItemEntity>> columns;
    /** The items whose dust is kept, or {@code null} to keep all. */
    private static Set<ItemEntity> keep;
    /** Set while the packet handler spawns kept item dust, so the new particles skip collisions. */
    private static boolean spawningItemDust;

    private ItemParticleBudget() {
    }

    /** Once per client tick: indexes the dropped items and chooses whose dust is kept. */
    public static void tick(Minecraft client) {
        columns = null;
        keep = null;
        if (client.level == null || client.player == null) return;
        List<ItemEntity> items = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item) items.add(item);
        }
        if (items.isEmpty()) return;
        Long2ObjectOpenHashMap<List<ItemEntity>> index = new Long2ObjectOpenHashMap<>();
        for (ItemEntity item : items) {
            index.computeIfAbsent(column(Mth.floor(item.getX()), Mth.floor(item.getZ())), k -> new ArrayList<>(2)).add(item);
        }
        columns = index;

        List<ItemEntity> pool = items;
        if (ItemPiles.enabled()) pool = new ArrayList<>(ItemPiles.representatives(items));
        int limit = HUDConfig.get().mapItemParticleLimit;
        if (limit > 0 && pool.size() > limit) {
            pool.sort((a, b) -> Double.compare(a.distanceToSqr(client.player), b.distanceToSqr(client.player)));
            pool = pool.subList(0, limit);
        }
        if (pool.size() < items.size()) {
            Set<ItemEntity> kept = Collections.newSetFromMap(new IdentityHashMap<>());
            kept.addAll(pool);
            keep = kept;
        }
    }

    public static Verdict classify(ClientboundLevelParticlesPacket packet) {
        Long2ObjectOpenHashMap<List<ItemEntity>> index = columns;
        if (index == null || !(packet.getParticle() instanceof DustParticleOptions)) return Verdict.NOT_ITEM_DUST;
        double x = packet.getX(), y = packet.getY(), z = packet.getZ();
        ItemEntity closest = null;
        double closestSq = SIDE * SIDE;
        for (int cx = Mth.floor(x - SIDE); cx <= Mth.floor(x + SIDE); cx++) {
            for (int cz = Mth.floor(z - SIDE); cz <= Mth.floor(z + SIDE); cz++) {
                List<ItemEntity> items = index.get(column(cx, cz));
                if (items == null) continue;
                for (ItemEntity item : items) {
                    double above = y - item.getY();
                    if (above < -BELOW || above > ABOVE) continue;
                    double dx = item.getX() - x, dz = item.getZ() - z;
                    double distanceSq = dx * dx + dz * dz;
                    if (distanceSq <= closestSq) {
                        closest = item;
                        closestSq = distanceSq;
                    }
                }
            }
        }
        if (closest == null) return Verdict.NOT_ITEM_DUST;
        Set<ItemEntity> kept = keep;
        return kept == null || kept.contains(closest) ? Verdict.SHOW : Verdict.HIDE;
    }

    public static void setSpawningItemDust(boolean spawning) {
        spawningItemDust = spawning;
    }

    public static boolean isSpawningItemDust() {
        return spawningItemDust;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
