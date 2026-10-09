package com.lootbeams.managers;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemPiles;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which items get a beam each frame. Items offer themselves as they are submitted, which is after frustum
 * culling; with piles on ({@link ItemPiles}) one item per pile and rarity is kept, and with a limit (HUD config
 * {@code lootBeamLimit}, 0 = all) only the nearest of those.
 */
public final class BeamBudget {
   private static final Map<ItemEntity, Double> CANDIDATES = new IdentityHashMap<>();
   private static Set<ItemEntity> allowed;

   private BeamBudget() {
   }

   public static void offer(ItemEntity entity, double distanceSq) {
      CANDIDATES.put(entity, distanceSq);
   }

   public static boolean allows(ItemEntity entity) {
      int limit = HUDConfig.get().lootBeamLimit;
      boolean piles = ItemPiles.enabled();
      if (!piles && (limit <= 0 || CANDIDATES.size() <= limit)) {
         return true;
      }
      if (allowed == null) {
         Map<ItemEntity, Double> pool = CANDIDATES;
         if (piles) {
            pool = new IdentityHashMap<>();
            for (ItemEntity kept : ItemPiles.representatives(CANDIDATES.keySet())) {
               pool.put(kept, CANDIDATES.get(kept));
            }
         }
         allowed = limit > 0 && pool.size() > limit ? nearest(pool, limit) : pool.keySet();
      }
      return allowed.contains(entity);
   }

   /** Clears the frame's candidates; called once the frame's beams are drawn. */
   public static void reset() {
      CANDIDATES.clear();
      allowed = null;
   }

   /** The {@code limit} keys with the smallest distance (all of them when there are fewer). */
   static <T> Set<T> nearest(Map<T, Double> distances, int limit) {
      List<Map.Entry<T, Double>> entries = new ArrayList<>(distances.entrySet());
      entries.sort(Map.Entry.comparingByValue());
      Set<T> result = Collections.newSetFromMap(new IdentityHashMap<>());
      for (int i = 0; i < Math.min(limit, entries.size()); i++) {
         result.add(entries.get(i).getKey());
      }
      return result;
   }
}
