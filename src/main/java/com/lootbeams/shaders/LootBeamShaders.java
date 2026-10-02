package com.lootbeams.shaders;

import com.lootbeams.LootBeams;
import com.lootbeams.render.CustomVertexFormats;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class LootBeamShaders {
   public static final BlendFunction LOOTBEAM_TRANSPARENCY = new BlendFunction(
         SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);

   private static final Map<Shader, RenderPipeline> SHADERS_MAP = new HashMap<>();
   private static final Map<DroplightShader, RenderPipeline> DROPLIGHT_SHADERS_MAP = new HashMap<>();
   private static final Map<String, RenderPipeline> CUSTOM_SHADERS_MAP = new HashMap<>();
   private static final Map<Identifier, RenderPipeline> REGISTERED_PIPELINES = new HashMap<>();
   private static boolean registered;

   public static RenderPipeline LOOT_BEAM_LIGHTNING;
   public static RenderPipeline LOOT_BEAM;
   public static RenderPipeline LOOT_BEAM_CENTER;
   public static RenderPipeline LOOT_BEAM_TRANSLUCENT;

   public static RenderPipeline getShader(Shader shader) {
      return SHADERS_MAP.get(shader);
   }

   public static RenderPipeline getShader(DroplightShader shader) {
      return DROPLIGHT_SHADERS_MAP.get(shader);
   }

   public static RenderPipeline getShader(CustomShader shader) {
      return CUSTOM_SHADERS_MAP.get(shader.name());
   }

   public static float getAverageColor(Shader shader) {
      return switch (shader) {
         case ADD -> 1.0F;
         case GLOW_OVERLAY -> 0.5F;
         case PARTICLE_OVERLAY -> 0.5F;
      };
   }

   public static void registerCoreShaders() {
      if (registered) {
         return;
      }
      registered = true;

      LOOT_BEAM_LIGHTNING = registerPipeline(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(LootBeams.id("pipeline/loot_beam_lightning"))
            .withVertexShader(Identifier.withDefaultNamespace("core/rendertype_lightning"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/rendertype_lightning"))
            .withBlend(LOOTBEAM_TRANSPARENCY)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
            .build());

      LOOT_BEAM = registerPipeline(RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation(LootBeams.id("pipeline/loot_beam"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .build());

      LOOT_BEAM_CENTER = registerPipeline(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(LootBeams.id("pipeline/loot_beam_center"))
            .withVertexShader(Identifier.withDefaultNamespace("core/rendertype_lightning"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/rendertype_lightning"))
            .withBlend(BlendFunction.LIGHTNING)
            .withCull(false)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .build());

      LOOT_BEAM_TRANSLUCENT = registerPipeline(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(LootBeams.id("pipeline/loot_beam_translucent"))
            .withShaderDefine("EMISSIVE")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .build());

      for (Shader shader : Shader.values()) {
         boolean particle = shader == Shader.PARTICLE_OVERLAY;
         SHADERS_MAP.put(shader, particle
               ? registerParticlePipeline(
                     "lootbeams_" + shader.name().toLowerCase(),
                     LootBeams.id("core/lootbeams_" + shader.name().toLowerCase()))
               : registerOverlayPipeline(
                     "lootbeams_" + shader.name().toLowerCase(),
                     LootBeams.id("core/lootbeams_" + shader.name().toLowerCase()),
                     DefaultVertexFormat.POSITION_TEX_COLOR,
                     false));
      }
      registerDroplightCoreShaders();
   }

   private static RenderPipeline registerTexturedPipeline(
         String name, Identifier shader, VertexFormat format, BlendFunction blend, boolean cull) {
      return registerPipeline(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(LootBeams.id("pipeline/" + name))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withSampler("Sampler0")
            .withBlend(blend)
            .withCull(cull)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .withVertexFormat(format, VertexFormat.Mode.QUADS)
            .build());
   }

   private static RenderPipeline registerOverlayPipeline(String name, Identifier shader, VertexFormat format, boolean cull) {
      return registerPipeline(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(LootBeams.id("pipeline/" + name))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(cull)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .withVertexFormat(format, VertexFormat.Mode.QUADS)
            .build());
   }

   private static RenderPipeline registerParticlePipeline(String name, Identifier shader) {
      return registerPipeline(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(LootBeams.id("pipeline/" + name))
            .withVertexShader(shader)
            .withFragmentShader(shader)
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(true)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withColorWrite(true, true)
            .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
            .build());
   }

   private static void registerDroplightCoreShader(DroplightShader shader, VertexFormat vertexFormat) {
      String registryShaderName = "lootbeams_" + shader.name().toLowerCase();
      DROPLIGHT_SHADERS_MAP.put(shader, registerTexturedPipeline(
            registryShaderName,
            LootBeams.id("core/" + registryShaderName),
            vertexFormat,
            BlendFunction.TRANSLUCENT,
            false));
   }

   public static void registerDroplightCoreShaders() {
      registerDroplightCoreShader(DroplightShader.DROPLIGHT, CustomVertexFormats.POSITION_TEX_COLOR0_COLOR1_CUSTOM);
      registerDroplightCoreShader(DroplightShader.DROPLIGHT_ANIMATED, CustomVertexFormats.POSITION_TEX_COLOR0_COLOR1_CUSTOM);
      registerDroplightCoreShader(DroplightShader.DROPLIGHT_GLOW, CustomVertexFormats.POSITION_TEX_COLOR0_COLOR1_CENTER);
   }

   public static void registerDroplightCoreShadersPublic() {
      registerCoreShaders();
   }

   private static void registerCustomCoreShader(CustomShader shader, VertexFormat vertexFormat) {
      String registryShaderName = shader.name().toLowerCase();
      CUSTOM_SHADERS_MAP.put(shader.name(), registerTexturedPipeline(
            "custom_" + registryShaderName,
            LootBeams.id("core/custom/" + registryShaderName),
            vertexFormat,
            BlendFunction.TRANSLUCENT,
            false));
   }

   public static String getFormattedName(String path) {
      String result = path;
      if (path.contains(".")) {
         result = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
      }
      return result.toUpperCase();
   }

   public static void onResourcesReload(ResourceManager resourceManager) {
      CustomShader.clearDynamicValues();
      CUSTOM_SHADERS_MAP.clear();
      Map<Identifier, Resource> resourceShaders = resourceManager.listResources(
            "shaders/core/custom", identifier -> identifier.getPath().endsWith(".json"));
      for (Identifier shaderId : resourceShaders.keySet()) {
         CustomShader newShader = CustomShader.addValue(getFormattedName(shaderId.getPath()));
         registerCustomCoreShader(newShader, CustomVertexFormats.POSITION_TEX_COLOR0_COLOR1_CENTER);
      }
   }

   private static RenderPipeline registerPipeline(RenderPipeline pipeline) {
      RenderPipeline existing = REGISTERED_PIPELINES.get(pipeline.getLocation());
      if (existing != null) {
         return existing;
      }
      RenderPipeline registeredPipeline = RenderPipelines.register(pipeline);
      REGISTERED_PIPELINES.put(registeredPipeline.getLocation(), registeredPipeline);
      return registeredPipeline;
   }

   public static class CustomShader {
      public static final CustomShader NONE = new CustomShader("NONE");
      private final String name;
      private static final List<CustomShader> dynamicValues = new ArrayList<>();
      private static final List<CustomShader> staticValues = new ArrayList<>();

      private CustomShader(String name) {
         this.name = name;
      }

      public String name() {
         return this.name;
      }

      public static CustomShader addValue(String name) {
         synchronized (dynamicValues) {
            for (CustomShader value : values()) {
               if (value.name.equals(name)) {
                  return value;
               }
            }
            CustomShader newValue = new CustomShader(name);
            dynamicValues.add(newValue);
            return newValue;
         }
      }

      public static void clearDynamicValues() {
         synchronized (dynamicValues) {
            dynamicValues.clear();
         }
      }

      public static List<CustomShader> values() {
         synchronized (dynamicValues) {
            List<CustomShader> allValues = new ArrayList<>(staticValues);
            allValues.addAll(dynamicValues);
            return allValues;
         }
      }

      public static CustomShader valueOf(String name) {
         for (CustomShader shader : staticValues) {
            if (shader.name.equals(name)) {
               return shader;
            }
         }
         synchronized (dynamicValues) {
            for (CustomShader shader : dynamicValues) {
               if (shader.name.equals(name)) {
                  return shader;
               }
            }
         }
         return addValue(name);
      }

      @Override
      public String toString() {
         return this.name;
      }

      static {
         staticValues.add(NONE);
      }
   }

   public enum DroplightShader {
      DROPLIGHT,
      DROPLIGHT_ANIMATED,
      DROPLIGHT_GLOW
   }

   public enum Shader {
      ADD,
      GLOW_OVERLAY,
      PARTICLE_OVERLAY
   }
}
