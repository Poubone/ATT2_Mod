package com.lootbeams.vfx;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;

public class VfxParticleGroup extends ParticleGroup<VFXParticle> {
   private static final ParticleGroupRenderState EMPTY = (collector, camera) -> {
   };

   public VfxParticleGroup(ParticleEngine engine) {
      super(engine);
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float tickDelta) {
      return EMPTY;
   }
}
