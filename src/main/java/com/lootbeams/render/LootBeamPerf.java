package com.lootbeams.render;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;

/**
 * The loot beam performance switches (Performance tab). Each one off restores the original behaviour of that
 * part alone, to measure it.
 */
public final class LootBeamPerf {
   private LootBeamPerf() {
   }

   /** One buffer per layer and cached layers; off: the shared buffer, a flush per beam layer, layers per call. */
   public static boolean batched() {
      return HUDConfig.get().batchLootBeams;
   }

   /** Frustum culling of items within the beam range; off: every item in range is drawn. */
   public static boolean culled() {
      return HUDConfig.get().cullLootBeams;
   }

   /** Default settings remembered per item; off: looked up again on every call. */
   public static boolean cachedConfig() {
      return HUDConfig.get().cacheLootBeamConfig;
   }

   /** Beam timers tidied once per frame; off: after every beam. */
   public static boolean prunedPerFrame() {
      return HUDConfig.get().pruneBeamTimersPerFrame;
   }

   /** Where beams, glows and particles are drawn: one buffer per layer, or the game's shared buffer. */
   public static BufferSource buffers() {
      return batched() ? LootBeamBufferSource.get() : Minecraft.getInstance().renderBuffers().bufferSource();
   }

   /** The original renderers drew each beam layer on its own; nothing to do when batching. */
   public static void flushIfOriginal(BufferSource buffer) {
      if (!batched()) {
         buffer.endBatch();
      }
   }
}
