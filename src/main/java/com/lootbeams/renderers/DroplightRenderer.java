package com.lootbeams.renderers;

import com.lootbeams.LootBeams;
import com.lootbeams.compat.iris.IrisCompat;
import com.lootbeams.compat.prism.PrismCompat;
import com.lootbeams.config.Configuration;
import com.lootbeams.extensions.LootbeamsBufferBuilder;
import com.lootbeams.helpers.ColorHelper;
import com.lootbeams.helpers.NumberHelper;
import com.lootbeams.helpers.RarityHelper;
import com.lootbeams.render.LootBeamRenderLayers;
import com.lootbeams.render.LootBeamShaderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Matrix4f;

public class DroplightRenderer {
   private static final Identifier DROPLIGHT_TEXTURE = LootBeams.id("textures/droplight/droplight.png");
   private static final Identifier DROPLIGHT_GLOW_TEXTURE = LootBeams.id("textures/droplight/droplight_glow.png");
   private static final Identifier DROPLIGHT_ANIMATED_TEXTURE = LootBeams.id("textures/droplight/droplight_animated.png");
   private static final Identifier DROPLIGHT_ANIMATED_BASE_TEXTURE = LootBeams.id("textures/droplight/droplight_animated_base.png");
   private static RenderType DROPLIGHT_LAYER = LootBeamRenderLayers.droplight(DROPLIGHT_TEXTURE);
   private static RenderType DROPLIGHT_GLOW_LAYER = LootBeamRenderLayers.droplight(DROPLIGHT_GLOW_TEXTURE);
   private static RenderType DROPLIGHT_ANIMATED_LAYER = LootBeamRenderLayers.droplightAnimated(DROPLIGHT_ANIMATED_TEXTURE);
   private static RenderType DROPLIGHT_BASE_LAYER = LootBeamRenderLayers.droplight(DROPLIGHT_ANIMATED_BASE_TEXTURE);
   private static boolean SHADERS_LOADED = IrisCompat.isShaderPackInUse();

   public static void renderBeam(
         BufferSource buffer,
         PoseStack matrixStack,
         ItemEntity itemEntity,
         Configuration itemConfig,
         TextColor color,
         float beamSizeMultiplier,
         float fadeAlpha,
         float currentGroundTime,
         long worldtime,
         float pticks) {
      if (!itemConfig.renderBeam) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      Camera camera = minecraft.gameRenderer.getMainCamera();
      boolean updatedShadersLoaded = IrisCompat.isShaderPackInUse();
      if (updatedShadersLoaded != SHADERS_LOADED) {
         SHADERS_LOADED = updatedShadersLoaded;
         DROPLIGHT_LAYER = LootBeamRenderLayers.droplight(DROPLIGHT_TEXTURE);
         DROPLIGHT_GLOW_LAYER = LootBeamRenderLayers.droplight(DROPLIGHT_GLOW_TEXTURE);
         DROPLIGHT_ANIMATED_LAYER = LootBeamRenderLayers.droplightAnimated(DROPLIGHT_ANIMATED_TEXTURE);
         DROPLIGHT_BASE_LAYER = LootBeamRenderLayers.droplight(DROPLIGHT_ANIMATED_BASE_TEXTURE);
      }

      float beamAlpha = itemConfig.beamAlpha * fadeAlpha;
      float beamHeight = itemConfig.beamHeight * beamSizeMultiplier;
      float beamGlowHeight = 2.5F * beamSizeMultiplier;
      if (beamHeight < itemConfig.minBeamHeight) {
         beamHeight = itemConfig.minBeamHeight;
         beamAlpha = beamHeight * 0.2F;
      }

      float beamWidth = itemConfig.beamRadius * beamSizeMultiplier;
      if (beamWidth < itemConfig.minBeamRadius) {
         beamWidth = itemConfig.minBeamRadius;
      }

      float yOffset = itemConfig.beamYOffset;
      if (itemConfig.commonShorterBeam && !RarityHelper.rarityCheck(itemEntity.getItem(), false)) {
         beamHeight *= 0.65F;
      }

      if (itemConfig.smoothBeamSize) {
         beamHeight = NumberHelper.smoothValue(beamHeight, currentGroundTime, itemConfig.smoothDuration);
         beamWidth = NumberHelper.smoothValue(beamWidth, currentGroundTime, itemConfig.smoothDuration);
         yOffset = NumberHelper.smoothValue(yOffset, currentGroundTime, itemConfig.smoothDuration);
      }

      TextColor color2 = getSecondColor(color, itemConfig.beamGradientModifiers);
      int color2Rgb = color2.getValue();
      // Official 1.21.4: RenderSystem.setShaderColor(1, 1, 1, beamAlpha).
      // 1.21.11 writes ColorModulator through DynamicUniforms; rgb carries Color1
      // because a second vertex colour is not a first-class attribute anymore.
      LootBeamShaderState.setColorModulator(
            (color2Rgb >> 16 & 0xFF) / 255.0F,
            (color2Rgb >> 8 & 0xFF) / 255.0F,
            (color2Rgb >> 0 & 0xFF) / 255.0F,
            beamAlpha);
      try {
         matrixStack.pushPose();
         matrixStack.translate(0.0, 0.015, 0.0);
         matrixStack.pushPose();
         matrixStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
         renderDroplightLayers(
               buffer,
               matrixStack,
               minecraft,
               itemEntity,
               itemConfig,
               color,
               color2,
               beamAlpha,
               beamWidth,
               beamHeight,
               beamGlowHeight,
               yOffset);
         matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));
         renderDroplightLayers(
               buffer,
               matrixStack,
               minecraft,
               itemEntity,
               itemConfig,
               color,
               color2,
               beamAlpha,
               beamWidth,
               beamHeight,
               beamGlowHeight,
               yOffset);
         matrixStack.popPose();
         matrixStack.popPose();
      } finally {
         LootBeamShaderState.clear();
      }
   }

   private static void renderDroplightLayers(
         BufferSource buffer,
         PoseStack matrixStack,
         Minecraft minecraft,
         ItemEntity itemEntity,
         Configuration itemConfig,
         TextColor color,
         TextColor color2,
         float beamAlpha,
         float beamWidth,
         float beamHeight,
         float beamGlowHeight,
         float yOffset) {
      VertexConsumer builder = buffer.getBuffer(DROPLIGHT_GLOW_LAYER);
      renderQuad(
            builder,
            matrixStack,
            color.getValue(),
            color2.getValue(),
            SHADERS_LOADED ? beamAlpha / 2.0F : beamAlpha,
            0.0F,
            yOffset,
            -0.001F,
            beamWidth,
            beamGlowHeight,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            0.5F,
            true,
            SHADERS_LOADED);
      buffer.endBatch();
      if (beamHeight <= 0.0F) {
         return;
      }
      if (itemConfig.animateDroplightBeam && !SHADERS_LOADED) {
         float halfBeamWidth = beamWidth / 2.0F;
         float oneThreeBeamWidth = beamWidth / 3.0F;
         float animationSpeed = itemConfig.droplightBeamAnimationSpeed;
         float itemAgeInSeconds = (itemEntity.getAge() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20.0F;
         builder = buffer.getBuffer(DROPLIGHT_ANIMATED_LAYER);
         renderAnimatedQuad(
               builder,
               matrixStack,
               color.getValue(),
               color2.getValue(),
               beamAlpha,
               0.0F,
               halfBeamWidth - oneThreeBeamWidth + yOffset,
               0.0F,
               beamWidth,
               beamHeight,
               0.0F,
               0.0F,
               1.0F,
               1.0F,
               true,
               animationSpeed,
               itemAgeInSeconds);
         buffer.endBatch();
         builder = buffer.getBuffer(DROPLIGHT_BASE_LAYER);
         renderQuad(
               builder,
               matrixStack,
               color.getValue(),
               color2.getValue(),
               beamAlpha,
               0.0F,
               -oneThreeBeamWidth + yOffset,
               0.0F,
               beamWidth,
               beamWidth,
               0.0F,
               0.0F,
               1.0F,
               1.0F,
               2.0F,
               false,
               SHADERS_LOADED);
         buffer.endBatch();
      } else {
         builder = buffer.getBuffer(DROPLIGHT_LAYER);
         renderQuad(
               builder,
               matrixStack,
               color.getValue(),
               color2.getValue(),
               beamAlpha,
               0.0F,
               yOffset,
               0.0F,
               beamWidth,
               beamHeight,
               0.0F,
               0.0F,
               1.0F,
               1.0F,
               2.0F,
               true,
               SHADERS_LOADED);
         buffer.endBatch();
      }
   }

   public static TextColor getSecondColor(float r, float g, float b, float a, List<String> modifiers) {
      int R = (int) (r * 255.0F);
      int G = (int) (g * 255.0F);
      int B = (int) (b * 255.0F);
      int A = (int) (a * 255.0F);
      return getSecondColor(TextColor.fromRgb(ColorHelper.build(A, R, G, B)), modifiers);
   }

   public static TextColor getSecondColor(TextColor color, List<String> modifiers) {
      if (PrismCompat.isPrismLoaded()) {
         return PrismCompat.applyModifiers(modifiers, color);
      }
      ColorHelper.Color newColor = new ColorHelper.Color(color.getValue()).applyModifiers(modifiers);
      return TextColor.fromRgb(newColor.getRgb() & 16777215);
   }

   private static void renderQuad(
         VertexConsumer builder,
         PoseStack matrixStack,
         int color,
         int color2,
         float alpha,
         float x,
         float y,
         float z,
         float w,
         float h,
         float u,
         float v,
         float u2,
         float v2,
         float alphaMultiplier,
         boolean fade,
         boolean shadersLoaded) {
      Pose stack = matrixStack.last();
      Matrix4f positionMatrix = stack.pose();
      float alpha2 = fade ? 0.0F : alpha;
      float red = (color >> 16 & 0xFF) / 255.0F;
      float green = (color >> 8 & 0xFF) / 255.0F;
      float blue = (color >> 0 & 0xFF) / 255.0F;
      int red2 = color2 >> 16 & 0xFF;
      int green2 = color2 >> 8 & 0xFF;
      int blue2 = color2 >> 0 & 0xFF;
      if (shadersLoaded) {
         builder.addVertex(positionMatrix, x - w / 2.0F, y, z)
               .setUv(u, v2)
               .setColor(red, green, blue, alpha)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(stack, 0.0F, 1.0F, 0.0F);
         builder.addVertex(positionMatrix, x + w / 2.0F, y, z)
               .setUv(u2, v2)
               .setColor(red, green, blue, alpha)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(stack, 0.0F, 1.0F, 0.0F);
         builder.addVertex(positionMatrix, x + w / 2.0F, y + h, z)
               .setUv(u2, v)
               .setColor(red, green, blue, alpha2)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(stack, 0.0F, 1.0F, 0.0F);
         builder.addVertex(positionMatrix, x - w / 2.0F, y + h, z)
               .setUv(u, v)
               .setColor(red, green, blue, alpha2)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(15728880)
               .setNormal(stack, 0.0F, 1.0F, 0.0F);
      } else {
         ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x - w / 2.0F, y, z).setUv(u, v2).setColor(red, green, blue, alpha))
               .color1(red2, green2, blue2, (int) (alpha * 255.0F))
               .longCustomData(alphaMultiplier, red2 / 255.0F, green2 / 255.0F, blue2 / 255.0F);
         ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x + w / 2.0F, y, z).setUv(u2, v2).setColor(red, green, blue, alpha))
               .color1(red2, green2, blue2, (int) (alpha * 255.0F))
               .longCustomData(alphaMultiplier, red2 / 255.0F, green2 / 255.0F, blue2 / 255.0F);
         ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x + w / 2.0F, y + h, z).setUv(u2, v).setColor(red, green, blue, alpha2))
               .color1(red2, green2, blue2, (int) (alpha2 * 255.0F))
               .longCustomData(alphaMultiplier, red2 / 255.0F, green2 / 255.0F, blue2 / 255.0F);
         ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x - w / 2.0F, y + h, z).setUv(u, v).setColor(red, green, blue, alpha2))
               .color1(red2, green2, blue2, (int) (alpha2 * 255.0F))
               .longCustomData(alphaMultiplier, red2 / 255.0F, green2 / 255.0F, blue2 / 255.0F);
      }
   }

   private static void renderAnimatedQuad(
         VertexConsumer builder,
         PoseStack matrixStack,
         int color,
         int color2,
         float alpha,
         float x,
         float y,
         float z,
         float w,
         float h,
         float u,
         float v,
         float u2,
         float v2,
         boolean fade,
         float animationSpeed,
         float itemAge) {
      Matrix4f positionMatrix = matrixStack.last().pose();
      float alpha2 = fade ? 0.0F : alpha;
      float red = (color >> 16 & 0xFF) / 255.0F;
      float green = (color >> 8 & 0xFF) / 255.0F;
      float blue = (color >> 0 & 0xFF) / 255.0F;
      int red2 = color2 >> 16 & 0xFF;
      int green2 = color2 >> 8 & 0xFF;
      int blue2 = color2 >> 0 & 0xFF;
      ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x - w / 2.0F, y, z).setUv(u, v2).setColor(red, green, blue, alpha))
            .color1(red2, green2, blue2, (int) (alpha * 255.0F))
            .longCustomData(w, h, animationSpeed, itemAge);
      ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x + w / 2.0F, y, z).setUv(u2, v2).setColor(red, green, blue, alpha))
            .color1(red2, green2, blue2, (int) (alpha * 255.0F))
            .longCustomData(w, h, animationSpeed, itemAge);
      ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x + w / 2.0F, y + h, z).setUv(u2, v).setColor(red, green, blue, alpha2))
            .color1(red2, green2, blue2, (int) (alpha2 * 255.0F))
            .longCustomData(w, h, animationSpeed, itemAge);
      ((LootbeamsBufferBuilder) builder.addVertex(positionMatrix, x - w / 2.0F, y + h, z).setUv(u, v).setColor(red, green, blue, alpha2))
            .color1(red2, green2, blue2, (int) (alpha2 * 255.0F))
            .longCustomData(w, h, animationSpeed, itemAge);
   }
}
