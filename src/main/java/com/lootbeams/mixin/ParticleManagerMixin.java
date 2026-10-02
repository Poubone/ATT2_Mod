package com.lootbeams.mixin;

import com.lootbeams.extensions.LootbeamsParticleManager;
import com.lootbeams.vfx.VFXParticle;
import com.lootbeams.vfx.VfxParticleGroup;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin implements LootbeamsParticleManager {
   @Shadow
   private Map<ParticleRenderType, ParticleGroup<?>> particles;

   @Inject(method = "createParticleGroup", at = @At("HEAD"), cancellable = true)
   private void lootbeams$createVfxGroup(ParticleRenderType type, CallbackInfoReturnable<ParticleGroup<?>> cir) {
      if (type == com.lootbeams.render.ParticleRenderType.VFX_PARTICLE) {
         cir.setReturnValue(new VfxParticleGroup((ParticleEngine) (Object) this));
      }
   }

   @Override
   public void renderCustomParticles(BufferSource builders, Camera camera, float tickDelta) {
      ParticleGroup<?> group = this.particles.get(com.lootbeams.render.ParticleRenderType.VFX_PARTICLE);
      if (group == null || group.isEmpty()) {
         return;
      }
      for (Object particle : group.getAll()) {
         if (particle instanceof VFXParticle vfx && vfx.isAlive()) {
            vfx.renderCustom(builders, camera, tickDelta);
         }
      }
      builders.endBatch();
   }
}
