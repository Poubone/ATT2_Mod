package com.lootbeams.managers;

import com.lootbeams.LootBeams;
import com.lootbeams.extensions.AnimatedTexture;
import com.lootbeams.vfx.VFXParticle;
import com.lootbeams.vfx.VFXParticleType;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.SpriteLoader.Preparations;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ParticleManager {
   public static final Identifier PARTICLES_PATH = LootBeams.id("particles");
   public static final Identifier ATLAS_ID = LootBeams.id("textures/atlas/particles.png");
   public static final Identifier GLOW_TEXTURE_ID = LootBeams.id("glow");
   public static final ParticleTexture GLOW_TEXTURE = ParticleTexture.of(GLOW_TEXTURE_ID);
   public static TextureAtlas ATLAS_TEXTURE = null;
   public static VFXParticleType GLOW_PARTICLE;

   public static void registerParticles() {
      GLOW_PARTICLE = new VFXParticleType(true);
   }

   public static void onResourceManagerReload(ResourceManager resourceManager, Executor prepareExecutor) {
      if (ATLAS_TEXTURE == null) {
         TextureAtlas atlasTexture = new TextureAtlas(ATLAS_ID);
         Minecraft.getInstance().getTextureManager().register(atlasTexture.location(), atlasTexture);
         ATLAS_TEXTURE = atlasTexture;
      }

      CompletableFuture<Preparations> completableFuture = SpriteLoader.create(ATLAS_TEXTURE)
            .loadAndStitch(
                  resourceManager,
                  PARTICLES_PATH,
                  0,
                  prepareExecutor,
                  Set.of(AnimationMetadataSection.TYPE, TextureMetadataSection.TYPE));
      ATLAS_TEXTURE.clearTextureData();
      Preparations stitchResult = completableFuture.join();
      stitchResult.readyForUpload().join();
      ATLAS_TEXTURE.upload(stitchResult);
   }

   public static void spawn(
         ParticleTexture texture,
         ItemStack itemStack,
         float r,
         float g,
         float b,
         float a,
         int lifetime,
         float size,
         Vec3 pos,
         Vec3 motion,
         Vec3 sourcePos) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return;
      }
      if (texture == null || texture.getSprite() == null) {
         return;
      }
      VFXParticle particle = new VFXParticle(
            mc.level, texture, itemStack, r, g, b, a, lifetime, size, pos, motion, 0.0F, false, true);
      particle.setParticleCenter(sourcePos);
      mc.particleEngine.add(particle);
   }

   public static List<ParticleTexture> getTextures() {
      if (ParticleTexture.getAtlasTexture() == null) {
         return List.of(GLOW_TEXTURE);
      }
      return ParticleTexture.getAnimatedTextures(ParticleTexture.getAtlasTexture(), ParticleTexture.class);
   }

   public static class ParticleTexture extends AnimatedTexture {
      public ParticleTexture(Identifier textureId) {
         super(textureId);
      }

      public static ParticleTexture of(Identifier textureId) {
         return of(textureId, ParticleTexture.class);
      }

      public static ParticleTexture of(String path, String namespace) {
         return of(path, namespace, ParticleTexture.class);
      }

      public static TextureAtlas getAtlasTexture() {
         return ParticleManager.ATLAS_TEXTURE;
      }

      @Override
      public TextureAtlas getSpriteAtlasTexture() {
         return getAtlasTexture();
      }
   }
}
