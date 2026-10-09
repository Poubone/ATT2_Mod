package com.lootbeams.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;

/**
 * LootBeams' buffer source. Every render type gets its own buffer the first time it is used, so switching
 * between beam, glow and particle layers no longer flushes: each pass ends with one draw per layer instead of
 * several draws per item.
 */
public final class LootBeamBufferSource extends MultiBufferSource.BufferSource {
   private static final LootBeamBufferSource INSTANCE = new LootBeamBufferSource();

   private LootBeamBufferSource() {
      super(new ByteBufferBuilder(256), new LinkedHashMap<>());
   }

   public static LootBeamBufferSource get() {
      return INSTANCE;
   }

   @Override
   public VertexConsumer getBuffer(RenderType renderType) {
      if (!this.fixedBuffers.containsKey(renderType)) {
         this.fixedBuffers.put(renderType, new ByteBufferBuilder(renderType.bufferSize()));
      }
      return super.getBuffer(renderType);
   }
}
