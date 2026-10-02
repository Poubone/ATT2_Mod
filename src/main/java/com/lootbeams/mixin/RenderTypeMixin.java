package com.lootbeams.mixin;

import com.lootbeams.render.LootBeamShaderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderType.class)
public class RenderTypeMixin {
   @ModifyArg(
         method = "draw",
         at = @At(
               value = "INVOKE",
               target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
         index = 1)
   private Vector4fc lootbeams$applyColorModulator(Vector4fc colorModulator) {
      return LootBeamShaderState.colorModulatorOr(colorModulator);
   }
}
