package com.lootbeams.renderers;

import com.lootbeams.LootBeams;
import com.lootbeams.config.Configuration;
import com.lootbeams.helpers.ColorHelper;
import com.lootbeams.helpers.NumberHelper;
import com.lootbeams.helpers.RarityHelper;
import com.lootbeams.render.LootBeamPerf;
import com.lootbeams.render.LootBeamRenderLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Matrix3f;

public class LootBeamRenderer {
   private static final Identifier LOOT_BEAM_TEXTURE = LootBeams.id("textures/lootbeam/lootbeam.png");
   private static final Identifier LOOT_BEAM_WHITE_TEXTURE = LootBeams.id("textures/lootbeam/lootbeam_white.png");
   private static final RenderType LOOT_BEAM_CENTER_LAYER = LootBeamRenderLayers.lootBeamCenter();
   private static RenderType LOOT_BEAM_LAYER = updateLootBeamLayer();

   public LootBeamRenderer() {
   }

   private static RenderType updateLootBeamLayer() {
      Identifier beamTexture = !LootBeams.config.solidBeam ? LOOT_BEAM_TEXTURE : LOOT_BEAM_WHITE_TEXTURE;
      return LootBeams.config.glowingBeam ? LootBeamRenderLayers.lootBeamLightning() : LootBeamRenderLayers.lootBeam(beamTexture);
   }

   public static void onConfigurationChange() {
      LOOT_BEAM_LAYER = updateLootBeamLayer();
   }

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
      float pticks
   ) {
      if (itemConfig.renderBeam) {
         ColorHelper.Color beamColor = ColorHelper.Color.of(color);
         ColorHelper.Color centerColor = ColorHelper.Color.of(color).applyModifiers(List.of("+r64", "+g64", "+b64"));
         float beamAlpha = itemConfig.beamAlpha * fadeAlpha;
         float glowBeamAlpha = beamAlpha * 0.4F;
         float beamHeight = itemConfig.beamHeight * beamSizeMultiplier;
         if (beamHeight < itemConfig.minBeamHeight) {
            beamHeight = itemConfig.minBeamHeight;
         }

         float beamRadius = 0.05F * itemConfig.beamRadius * beamSizeMultiplier;
         float minBeamRadius = 0.05F * itemConfig.minBeamRadius;
         if (beamRadius < minBeamRadius) {
            beamRadius = minBeamRadius;
         }

         float beamGlowRadius = beamRadius + beamRadius * 0.2F;
         float yOffset = itemConfig.beamYOffset;
         if (itemConfig.commonShorterBeam && !RarityHelper.rarityCheck(itemEntity.getItem(), false)) {
            beamHeight *= 0.65F;
         }

         if (itemConfig.smoothBeamSize) {
            beamHeight = NumberHelper.smoothValue(beamHeight, currentGroundTime, itemConfig.smoothDuration);
            yOffset = NumberHelper.smoothValue(yOffset, currentGroundTime, itemConfig.smoothDuration);
            beamRadius = NumberHelper.smoothValue(beamRadius, currentGroundTime, itemConfig.smoothDuration);
            beamGlowRadius = NumberHelper.smoothValue(beamGlowRadius, currentGroundTime, itemConfig.smoothDuration);
         }

         matrixStack.pushPose();
         float rotation = (float)Math.floorMod(worldtime, 40L) + pticks;
         matrixStack.mulPose(Axis.YP.rotationDegrees(rotation * 2.25F - 45.0F));
         renderBeamLayer(
            matrixStack,
            buffer.getBuffer(LOOT_BEAM_LAYER),
            beamColor,
            beamAlpha,
            0.0F,
            beamHeight,
            0.0F,
            beamRadius,
            beamRadius,
            0.0F,
            -beamRadius,
            0.0F,
            0.0F,
            -beamRadius,
            itemConfig.solidBeam,
            yOffset
         );
         matrixStack.popPose();
         renderBeamLayer(
            matrixStack,
            buffer.getBuffer(LOOT_BEAM_LAYER),
            beamColor,
            glowBeamAlpha,
            0.0F,
            beamHeight,
            -beamGlowRadius,
            -beamGlowRadius,
            beamGlowRadius,
            -beamGlowRadius,
            -beamGlowRadius,
            beamGlowRadius,
            beamGlowRadius,
            beamGlowRadius,
            itemConfig.solidBeam,
            yOffset
         );
         LootBeamPerf.flushIfOriginal(buffer);
         if (itemConfig.whiteCenter) {
            renderBeamLayer(
               matrixStack,
               buffer.getBuffer(LOOT_BEAM_CENTER_LAYER),
               centerColor,
               beamAlpha,
               0.0F,
               beamHeight,
               0.0F,
               beamRadius * 0.4F,
               beamRadius * 0.4F,
               0.0F,
               -beamRadius * 0.4F,
               0.0F,
               0.0F,
               -beamRadius * 0.4F,
               itemConfig.solidBeam,
               yOffset
            );
            LootBeamPerf.flushIfOriginal(buffer);
         }
      }
   }

   private static void renderBeamLayer(
      PoseStack stack,
      VertexConsumer builder,
      ColorHelper.Color color,
      float alpha,
      float y,
      float height,
      float radius_1,
      float radius_2,
      float radius_3,
      float radius_4,
      float radius_5,
      float radius_6,
      float radius_7,
      float radius_8,
      boolean solidBeam,
      float yOffset
   ) {
      float halfHeight = height / 2.0F;
      stack.pushPose();
      stack.translate(0.0F, yOffset, 0.0F);
      if (!solidBeam) {
         stack.translate(0.0F, halfHeight, 0.0F);
         stack.mulPose(Axis.XP.rotationDegrees(180.0F));
      }

      renderPart(
         stack, builder, color.fR, color.fG, color.fB, alpha, y, height, radius_1, radius_2, radius_3, radius_4, radius_5, radius_6, radius_7, radius_8, false
      );
      if (!solidBeam) {
         stack.mulPose(Axis.XP.rotationDegrees(-180.0F));
      }

      renderPart(
         stack,
         builder,
         color.fR,
         color.fG,
         color.fB,
         alpha,
         y,
         height,
         radius_1,
         radius_2,
         radius_3,
         radius_4,
         radius_5,
         radius_6,
         radius_7,
         radius_8,
         solidBeam
      );
      stack.popPose();
   }

   private static void renderPart(
      PoseStack stack,
      VertexConsumer builder,
      float red,
      float green,
      float blue,
      float alpha,
      float y,
      float height,
      float radius_1,
      float radius_2,
      float radius_3,
      float radius_4,
      float radius_5,
      float radius_6,
      float radius_7,
      float radius_8,
      boolean gradient
   ) {
      if (gradient) {
         renderGradientPart(stack, builder, red, green, blue, alpha, y, height, radius_1, radius_2, radius_3, radius_4, radius_5, radius_6, radius_7, radius_8);
      } else {
         renderPart(stack, builder, red, green, blue, alpha, y, height, radius_1, radius_2, radius_3, radius_4, radius_5, radius_6, radius_7, radius_8);
      }
   }

   private static void renderPart(
      PoseStack stack,
      VertexConsumer builder,
      float red,
      float green,
      float blue,
      float alpha,
      float y,
      float height,
      float radius_1,
      float radius_2,
      float radius_3,
      float radius_4,
      float radius_5,
      float radius_6,
      float radius_7,
      float radius_8
   ) {
      Pose matrixentry = stack.last();
      Matrix3f matrixnormal = matrixentry.normal();
      renderQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_1, radius_2, radius_3, radius_4);
      renderQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_7, radius_8, radius_5, radius_6);
      renderQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_3, radius_4, radius_7, radius_8);
      renderQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_5, radius_6, radius_1, radius_2);
   }

   private static void renderGradientPart(
      PoseStack stack,
      VertexConsumer builder,
      float red,
      float green,
      float blue,
      float alpha,
      float y,
      float height,
      float radius_1,
      float radius_2,
      float radius_3,
      float radius_4,
      float radius_5,
      float radius_6,
      float radius_7,
      float radius_8
   ) {
      Pose matrixentry = stack.last();
      Matrix3f matrixnormal = matrixentry.normal();
      renderUpwardsGradientQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_1, radius_2, radius_3, radius_4);
      renderUpwardsGradientQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_7, radius_8, radius_5, radius_6);
      renderUpwardsGradientQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_3, radius_4, radius_7, radius_8);
      renderUpwardsGradientQuad(matrixentry, matrixnormal, builder, red, green, blue, alpha, y, height, radius_5, radius_6, radius_1, radius_2);
   }

   private static void renderQuad(
      Pose stack,
      Matrix3f normal,
      VertexConsumer builder,
      float red,
      float green,
      float blue,
      float alpha,
      float y,
      float height,
      float x1,
      float z1,
      float x0,
      float z0
   ) {
      addVertex(builder, stack, normal, red, green, blue, alpha, y + height / 2.0F, x1, z1, 1.0F, 0.0F);
      addVertex(builder, stack, normal, red, green, blue, alpha, y, x1, z1, 1.0F, 1.0F);
      addVertex(builder, stack, normal, red, green, blue, alpha, y, x0, z0, 0.0F, 1.0F);
      addVertex(builder, stack, normal, red, green, blue, alpha, y + height / 2.0F, x0, z0, 0.0F, 0.0F);
   }

   private static void renderUpwardsGradientQuad(
      Pose stack,
      Matrix3f normal,
      VertexConsumer builder,
      float red,
      float green,
      float blue,
      float alpha,
      float y,
      float height,
      float z1,
      float texu1,
      float z,
      float texu
   ) {
      addVertex(builder, stack, normal, red, green, blue, 0.0F, y + height, z1, texu1, 1.0F, 0.0F);
      addVertex(builder, stack, normal, red, green, blue, alpha, y + height / 2.0F, z1, texu1, 1.0F, 1.0F);
      addVertex(builder, stack, normal, red, green, blue, alpha, y + height / 2.0F, z, texu, 0.0F, 1.0F);
      addVertex(builder, stack, normal, red, green, blue, 0.0F, y + height, z, texu, 0.0F, 0.0F);
   }

   private static void addVertex(
      VertexConsumer builder, Pose stack, Matrix3f normal, float red, float green, float blue, float alpha, float y, float x, float z, float texu, float texv
   ) {
      builder.addVertex(stack.pose(), x, y, z)
         .setColor(red, green, blue, alpha)
         .setUv(texu, texv)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(stack, 0.0F, 1.0F, 0.0F);
   }
}
