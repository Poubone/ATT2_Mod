package com.lootbeams.managers;

import com.lootbeams.LootBeams;
import com.lootbeams.extensions.AnimatedTexture;
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

public class GlowEffectManager {
   public static final Identifier GLOW_EFFECTS_PATH = LootBeams.id("glow_effects");
   public static final Identifier ATLAS_ID = LootBeams.id("textures/atlas/glow_effects.png");
   public static final Identifier GLOW_TEXTURE_ID = LootBeams.id("glow");
   public static final Identifier GLOW_SHADER_TEXTURE_ID = LootBeams.id("glow_shaders");
   public static final GlowEffectTexture GLOW_TEXTURE = GlowEffectTexture.of(GLOW_TEXTURE_ID);
   public static final GlowEffectTexture GLOW_SHADER_TEXTURE = GlowEffectTexture.of(GLOW_SHADER_TEXTURE_ID);
   public static TextureAtlas ATLAS_TEXTURE = null;

   public static void onResourceManagerReload(ResourceManager resourceManager, Executor prepareExecutor) {
      if (ATLAS_TEXTURE == null) {
         TextureAtlas atlasTexture = new TextureAtlas(ATLAS_ID);
         Minecraft.getInstance().getTextureManager().register(atlasTexture.location(), atlasTexture);
         ATLAS_TEXTURE = atlasTexture;
      }

      CompletableFuture<Preparations> completableFuture = SpriteLoader.create(ATLAS_TEXTURE)
            .loadAndStitch(
                  resourceManager,
                  GLOW_EFFECTS_PATH,
                  0,
                  prepareExecutor,
                  Set.of(AnimationMetadataSection.TYPE, TextureMetadataSection.TYPE));
      ATLAS_TEXTURE.clearTextureData();
      Preparations stitchResult = completableFuture.join();
      stitchResult.readyForUpload().join();
      ATLAS_TEXTURE.upload(stitchResult);
   }

   public static List<GlowEffectTexture> getTextures() {
      if (GlowEffectTexture.getAtlasTexture() == null) {
         return List.of(GLOW_TEXTURE, GLOW_SHADER_TEXTURE);
      }
      return GlowEffectTexture.getAnimatedTextures(GlowEffectTexture.getAtlasTexture(), GlowEffectTexture.class);
   }

   public static class GlowEffectTexture extends AnimatedTexture {
      public GlowEffectTexture(Identifier textureId) {
         super(textureId);
      }

      public static GlowEffectTexture of(Identifier textureId) {
         return of(textureId, GlowEffectTexture.class);
      }

      public static GlowEffectTexture of(String path, String namespace) {
         return of(path, namespace, GlowEffectTexture.class);
      }

      public static TextureAtlas getAtlasTexture() {
         return GlowEffectManager.ATLAS_TEXTURE;
      }

      @Override
      public TextureAtlas getSpriteAtlasTexture() {
         return getAtlasTexture();
      }
   }
}
