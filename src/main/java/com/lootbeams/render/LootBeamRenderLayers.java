package com.lootbeams.render;

import com.lootbeams.compat.iris.IrisCompat;
import com.lootbeams.shaders.LootBeamShaders;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LootBeamRenderLayers {
   /**
    * Layers asked for per particle or per glow, built once per distinct argument set. A fresh layer per call
    * made every particle a separate draw (a new layer object ends the previous batch) and allocated each frame.
    */
   private static final Map<List<Object>, RenderType> CACHE = new HashMap<>();

   private static RenderType cached(Supplier<RenderType> factory, Object... key) {
      if (!LootBeamPerf.batched()) {
         return factory.get(); // the original built a new layer on every call
      }
      List<Object> fullKey = new ArrayList<>(Arrays.asList(key));
      fullKey.add(IrisCompat.isShaderPackInUse());
      return CACHE.computeIfAbsent(fullKey, k -> factory.get());
   }

   public static RenderType lootBeamLightning() {
      return RenderType.create("loot_beam_lightning", RenderSetup.builder(LootBeamShaders.LOOT_BEAM_LIGHTNING)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType lootBeam(Identifier texture) {
      return RenderType.create("loot_beam/" + texture, RenderSetup.builder(LootBeamShaders.LOOT_BEAM)
            .withTexture("Sampler0", texture)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType lootBeamCenter() {
      return RenderType.create("loot_beam_center", RenderSetup.builder(LootBeamShaders.LOOT_BEAM_CENTER)
            .setOutputTarget(OutputTarget.WEATHER_TARGET)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType droplight(Identifier texture) {
      if (IrisCompat.isShaderPackInUse()) {
         return RenderTypes.entityTranslucentEmissive(texture, false);
      }
      RenderPipeline pipeline = LootBeamShaders.getShader(LootBeamShaders.DroplightShader.DROPLIGHT);
      return RenderType.create("loot_beam_droplight/" + texture, RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType droplightAnimated(Identifier texture) {
      RenderPipeline pipeline = LootBeamShaders.getShader(LootBeamShaders.DroplightShader.DROPLIGHT_ANIMATED);
      return RenderType.create("loot_beam_droplight_animated/" + texture, RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType particles(Identifier texture, LootBeamShaders.Shader shader) {
      return cached(() -> createParticles(texture, shader), "particles", texture, shader);
   }

   private static RenderType createParticles(Identifier texture, LootBeamShaders.Shader shader) {
      if (IrisCompat.isShaderPackInUse()) {
         return RenderTypes.entityTranslucentEmissive(texture, false);
      }
      RenderPipeline pipeline = LootBeamShaders.getShader(shader);
      return RenderType.create("loot_beam_particles/" + shader.name() + "/" + texture, RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .useLightmap()
            .setOutputTarget(OutputTarget.MAIN_TARGET)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType groundGlowEffect(
         Identifier texture, boolean isColored, boolean useGlowGradient, LootBeamShaders.CustomShader customShader) {
      return cached(() -> createGroundGlowEffect(texture, isColored, useGlowGradient, customShader),
            "ground_glow", texture, isColored, useGlowGradient, customShader);
   }

   private static RenderType createGroundGlowEffect(
         Identifier texture, boolean isColored, boolean useGlowGradient, LootBeamShaders.CustomShader customShader) {
      if (IrisCompat.isShaderPackInUse()) {
         return RenderTypes.entityTranslucentEmissive(texture, false);
      }
      RenderPipeline pipeline;
      if (customShader != LootBeamShaders.CustomShader.NONE && LootBeamShaders.getShader(customShader) != null) {
         pipeline = LootBeamShaders.getShader(customShader);
      } else if (isColored) {
         pipeline = LootBeamShaders.getShader(LootBeamShaders.Shader.GLOW_OVERLAY);
      } else if (useGlowGradient) {
         pipeline = LootBeamShaders.getShader(LootBeamShaders.DroplightShader.DROPLIGHT_GLOW);
      } else {
         pipeline = LootBeamShaders.getShader(LootBeamShaders.Shader.ADD);
      }
      return RenderType.create("loot_beam_ground_glow/" + texture, RenderSetup.builder(pipeline)
            .withTexture("Sampler0", texture)
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }

   public static RenderType translucentNoCull(Identifier texture) {
      return cached(() -> createTranslucentNoCull(texture), "translucent_no_cull", texture);
   }

   private static RenderType createTranslucentNoCull(Identifier texture) {
      return RenderType.create("loot_beam_translucent/" + texture, RenderSetup.builder(LootBeamShaders.LOOT_BEAM_TRANSLUCENT)
            .withTexture("Sampler0", texture)
            .useOverlay()
            .useLightmap()
            .sortOnUpload()
            .bufferSize(1536)
            .createRenderSetup());
   }
}
