package com.lootbeams.vfx;

import com.lootbeams.managers.ParticleManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class VFXParticleType extends SimpleParticleType {
   public ParticleManager.ParticleTexture texture;
   public float red;
   public float green;
   public float blue;
   public float alpha;
   public int lifetime;
   public float size;
   public Vec3 sourcePos;
   public float gravity;
   public boolean collision;
   public boolean fullbright;
   public ItemStack itemStack;

   public VFXParticleType(boolean alwaysShow) {
      super(alwaysShow);
   }

   public VFXParticleType setTexture(ParticleManager.ParticleTexture texture) {
      this.texture = texture;
      return this;
   }

   public VFXParticleType setColor(float red, float green, float blue, float alpha) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.alpha = alpha;
      return this;
   }

   public VFXParticleType setLifetime(int lifetime) {
      this.lifetime = lifetime;
      return this;
   }

   public VFXParticleType setSize(float size) {
      this.size = size;
      return this;
   }

   public VFXParticleType setSourcePos(Vec3 sourcePos) {
      this.sourcePos = sourcePos;
      return this;
   }

   public VFXParticleType setGravity(float gravity) {
      this.gravity = gravity;
      return this;
   }

   public VFXParticleType setCollision(boolean collision) {
      this.collision = collision;
      return this;
   }

   public VFXParticleType setFullbright(boolean fullbright) {
      this.fullbright = fullbright;
      return this;
   }

   public VFXParticleType setItemStack(ItemStack itemStack) {
      this.itemStack = itemStack;
      return this;
   }
}
