package com.lootbeams.render;

import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * 1.21.11 replacement for {@code RenderSystem.setShaderColor}.
 *
 * <p>Vanilla {@code RenderType.draw} always writes {@code ColorModulator = (1,1,1,1)}
 * into {@code DynamicTransforms}. The official 1.21.4 Droplight path called
 * {@code setShaderColor(1, 1, 1, beamAlpha)} around {@code endBatch()}. On 1.21.11
 * that API is gone, so this thread-local is read by {@code RenderTypeMixin} and
 * injected into {@code DynamicUniforms.writeTransform}.
 *
 * <p>{@code rgb} carries the second Droplight colour (official {@code Color1})
 * because 1.21.11 rejects a second {@code Usage.COLOR} vertex element. {@code a}
 * is the official beam alpha.
 */
public final class LootBeamShaderState {
   private static final ThreadLocal<Vector4f> COLOR_MODULATOR = new ThreadLocal<>();

   private LootBeamShaderState() {
   }

   public static void setColorModulator(float red, float green, float blue, float alpha) {
      COLOR_MODULATOR.set(new Vector4f(red, green, blue, alpha));
   }

   public static void clear() {
      COLOR_MODULATOR.remove();
   }

   public static Vector4fc colorModulatorOr(Vector4fc fallback) {
      Vector4f current = COLOR_MODULATOR.get();
      return current != null ? current : fallback;
   }
}
