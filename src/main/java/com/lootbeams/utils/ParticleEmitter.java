package com.lootbeams.utils;

import com.lootbeams.config.Configuration;
import com.lootbeams.helpers.ColorHelper;
import com.lootbeams.helpers.RarityHelper;
import com.lootbeams.managers.ParticleManager;
import com.lootbeams.vfx.VFXParticle;
import com.lootbeams.vfx.VFXParticleType;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

public class ParticleEmitter {
   private static final Random RANDOM = new Random();
   private static final float TICK_PER_SECOND = 20.0F;
   private static final WeakHashMap<ItemEntity, Float> nextParticleSpawnTicks = new WeakHashMap<>();

   public ParticleEmitter() {
   }

   public static void createParticlesForItem(ItemEntity itemEntity, Configuration itemConfig, int entityTime, TextColor color, float alpha, float pticks) {
      if (itemConfig.particles) {
         if (!itemConfig.particleRareOnly || RarityHelper.rarityCheck(itemEntity.getItem(), false)) {
            if (!Minecraft.getInstance().isPaused()) {
               if (!nextParticleSpawnTicks.containsKey(itemEntity)) {
                  nextParticleSpawnTicks.put(itemEntity, 20.0F / itemConfig.particleCount);
               }

               ColorHelper.Color particleColor = ColorHelper.Color.of(color);
               float particleCount = itemConfig.particleCount;
               float currentTick = entityTime % 20.0F;
               float currentTickValue = currentTick + pticks;
               float ticksPerOneParticle = 20.0F / particleCount;
               if (currentTick == 0.0F && nextParticleSpawnTicks.get(itemEntity) >= 20.0F) {
                  nextParticleSpawnTicks.put(itemEntity, ticksPerOneParticle);
               }

               if (currentTickValue >= nextParticleSpawnTicks.get(itemEntity)) {
                  nextParticleSpawnTicks.put(itemEntity, ((float)Math.floor(currentTickValue / ticksPerOneParticle) + 1.0F) * ticksPerOneParticle);
                  float particleSize = itemConfig.particleRandomSize
                     ? RANDOM.nextFloat(0.25F * itemConfig.particleSize, 1.1F * itemConfig.particleSize)
                     : itemConfig.particleSize;
                  float particleSpeed = itemConfig.particleSpeed;
                  float particleRadius = itemConfig.particleRadius;
                  float randomnessIntensity = itemConfig.randomnessIntensity;
                  Vec3 randomDir = new Vec3(
                        RANDOM.nextDouble(-particleSpeed / 2.0F, particleSpeed / 2.0F),
                        itemConfig.particleRandomY ? RANDOM.nextDouble(particleSpeed / 2.0F, particleSpeed) : particleSpeed,
                        RANDOM.nextDouble(-particleSpeed / 2.0F, particleSpeed / 2.0F)
                     )
                     .multiply(randomnessIntensity, randomnessIntensity, randomnessIntensity);
                  Vec3 particleDir = new Vec3(itemConfig.particleDirectionX, itemConfig.particleDirectionY, itemConfig.particleDirectionZ).multiply(randomDir);
                  ParticleManager.ParticleTexture texture = itemConfig.particleTexture;
                  double particleY = itemConfig.particleRandomY
                     ? RANDOM.nextDouble(
                        itemEntity.getY() + itemConfig.particleYOffset - particleRadius / 3.0F,
                        itemEntity.getY() + itemConfig.particleYOffset + particleRadius / 3.0F
                     )
                     : itemEntity.getY() + itemConfig.particleYOffset + particleSize / 10.0F;
                  addParticle(
                     texture,
                     particleColor.fR,
                     particleColor.fG,
                     particleColor.fB,
                     alpha,
                     itemConfig.particleLifetime,
                     particleSize,
                     new Vec3(
                        RANDOM.nextDouble(itemEntity.getX() - particleRadius, itemEntity.getX() + particleRadius),
                        particleY,
                        RANDOM.nextDouble(itemEntity.getZ() - particleRadius, itemEntity.getZ() + particleRadius)
                     ),
                     particleDir,
                     itemEntity
                  );
               }
            }
         }
      }
   }

   private static void addParticle(
      ParticleManager.ParticleTexture texture,
      float red,
      float green,
      float blue,
      float alpha,
      int lifetime,
      float size,
      Vec3 pos,
      Vec3 motion,
      ItemEntity itemEntity
   ) {
      Minecraft mc = Minecraft.getInstance();
      alpha *= 1.5F;
      if (mc.level == null || mc.particleEngine == null || ParticleManager.GLOW_PARTICLE == null) {
         return;
      }
      VFXParticleType glowParticle = ParticleManager.GLOW_PARTICLE
            .setTexture(texture)
            .setColor(red, green, blue, alpha)
            .setLifetime(lifetime)
            .setSize(size)
            .setSourcePos(itemEntity.position())
            .setGravity(0.0F)
            .setCollision(false)
            .setFullbright(true)
            .setItemStack(itemEntity.getItem());
      Particle particle = new VFXParticle.Factory(null).createParticle(
            glowParticle,
            mc.level,
            pos.x,
            pos.y,
            pos.z,
            motion.x,
            motion.y,
            motion.z,
            mc.level.random);
      if (particle != null) {
         mc.particleEngine.add(particle);
      }
   }
}
