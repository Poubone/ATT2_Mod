package com.lootbeams.managers;

import com.lootbeams.contexts.WorldRendererContext;
import com.lootbeams.extensions.LootbeamsParticleManager;
import com.lootbeams.render.BeamRender;
import com.lootbeams.render.LootBeamBufferSource;
import com.lootbeams.render.LootBeamPerf;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;

public class RenderManager {
   private static final List<BiConsumer<PoseStack, BufferSource>> RENDER_BEFORE_PARTICLES = new ArrayList<>();
   private static final List<BiConsumer<PoseStack, BufferSource>> RENDER_AFTER_TRANSLUCENT = new ArrayList<>();
   private static final List<BiConsumer<PoseStack, BufferSource>> RENDER_AFTER_WEATHER = new ArrayList<>();
   private static final List<BiConsumer<PoseStack, BufferSource>> RENDER_BEFORE_END = new ArrayList<>();

   public RenderManager() {
   }

   public static void onWorldRenderBeforeParticles(WorldRendererContext worldRendererContext) {
      renderBeforeParticles(worldRendererContext.getMatrixStack(), worldRendererContext.getCamera().position(), worldRendererContext.getConsumers());
   }

   public static void onWorldRenderAfterTranslucent(WorldRendererContext worldRendererContext) {
      renderAfterTranslucent(worldRendererContext.getMatrixStack(), worldRendererContext.getCamera().position(), worldRendererContext.getConsumers());
   }

   public static void onWorldRenderAfterWeather(WorldRendererContext worldRendererContext) {
      renderAfterWeather(worldRendererContext.getMatrixStack(), worldRendererContext.getCamera().position(), worldRendererContext.getConsumers());
      if (Minecraft.getInstance().particleEngine instanceof LootbeamsParticleManager particleManager
            && worldRendererContext.getConsumers() != null
            && worldRendererContext.getCamera() != null) {
         float tickDelta = worldRendererContext.getTickCounter() != null
               ? worldRendererContext.getTickCounter().getGameTimeDeltaPartialTick(false)
               : Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
         particleManager.renderCustomParticles(
               LootBeamPerf.buffers(), worldRendererContext.getCamera(), tickDelta);
      }
   }

   public static void onWorldRenderBeforeEnd(WorldRendererContext worldRendererContext) {
      renderBeforeEnd(worldRendererContext.getMatrixStack(), worldRendererContext.getCamera().position(), worldRendererContext.getConsumers());
   }

   public static void onWorldRenderEnd(WorldRendererContext worldRendererContext) {
      RENDER_BEFORE_PARTICLES.clear();
      RENDER_AFTER_TRANSLUCENT.clear();
      RENDER_AFTER_WEATHER.clear();
      RENDER_BEFORE_END.clear();
      BeamBudget.reset();
      BeamRender.pruneGroundTimes();
   }

   public static void renderAfter(PoseStack stack, Vec3 cameraPos, BufferSource immediate, List<BiConsumer<PoseStack, BufferSource>> consumers) {
      if (stack != null && immediate != null) {
         stack.pushPose();
         stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
         consumers.forEach(consumer -> consumer.accept(stack, immediate));
         stack.popPose();
         LootBeamBufferSource.get().endBatch();
         immediate.endBatch();
      }
   }

   public static void renderBeforeParticles(PoseStack stack, Vec3 cameraPos, BufferSource immediate) {
      renderAfter(stack, cameraPos, immediate, RENDER_BEFORE_PARTICLES);
   }

   public static void renderAfterTranslucent(PoseStack stack, Vec3 cameraPos, BufferSource immediate) {
      renderAfter(stack, cameraPos, immediate, RENDER_AFTER_TRANSLUCENT);
   }

   public static void renderAfterWeather(PoseStack stack, Vec3 cameraPos, BufferSource immediate) {
      renderAfter(stack, cameraPos, immediate, RENDER_AFTER_WEATHER);
   }

   public static void renderBeforeEnd(PoseStack stack, Vec3 cameraPos, BufferSource immediate) {
      renderAfter(stack, cameraPos, immediate, RENDER_BEFORE_END);
   }

   public static void addRenderBeforeParticles(BiConsumer<PoseStack, BufferSource> render) {
      RENDER_BEFORE_PARTICLES.add(render);
   }

   public static void addRenderAfterTranslucent(BiConsumer<PoseStack, BufferSource> render) {
      RENDER_AFTER_TRANSLUCENT.add(render);
   }

   public static void addRenderAfterWeather(BiConsumer<PoseStack, BufferSource> render) {
      RENDER_AFTER_WEATHER.add(render);
   }

   public static void addRenderBeforeEnd(BiConsumer<PoseStack, BufferSource> render) {
      RENDER_BEFORE_END.add(render);
   }
}
