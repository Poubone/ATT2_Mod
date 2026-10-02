package com.lootbeams.renderers;

import com.lootbeams.compat.iris.IrisCompat;
import com.lootbeams.config.Configuration;
import com.lootbeams.extensions.LootbeamsBufferBuilder;
import com.lootbeams.helpers.ColorHelper;
import com.lootbeams.helpers.NumberHelper;
import com.lootbeams.managers.GlowEffectManager;
import com.lootbeams.render.LootBeamRenderLayers;
import com.lootbeams.shaders.LootBeamShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Matrix4f;

public class GroundEffectRenderer {
   public static void renderGroundEffect(
         MultiBufferSource buffer,
         PoseStack matrixStack,
         ItemEntity itemEntity,
         Configuration itemConfig,
         TextColor color,
         float sizeMultiplier,
         float fadeAlpha,
         float currentGroundTime,
         long worldtime,
         float pticks) {
      ColorHelper.Color effectColor = ColorHelper.Color.of(color);
      if (!itemConfig.glowEffect) {
         return;
      }
      matrixStack.pushPose();
      matrixStack.translate(0.0F, 0.001F, 0.0F);
      if (itemConfig.rotateGlow) {
         float rotationSpeed = itemConfig.glowRotationSpeed;
         float rotation = currentGroundTime * rotationSpeed % 360.0F;
         float rotationDirection = itemConfig.glowRotateClockwise ? -1.0F : 1.0F;
         matrixStack.mulPose(Axis.YP.rotationDegrees(rotation * rotationDirection));
      }

      TextureAtlasSprite glowEffectSprite = itemConfig.glowEffectTexture.getSprite();
      if (IrisCompat.isShaderPackInUse() && itemConfig.glowEffectTexture == GlowEffectManager.GLOW_TEXTURE) {
         glowEffectSprite = GlowEffectManager.GLOW_SHADER_TEXTURE.getSprite();
      }
      if (!IrisCompat.isShaderPackInUse() && itemConfig.glowEffectTexture == GlowEffectManager.GLOW_SHADER_TEXTURE) {
         glowEffectSprite = GlowEffectManager.GLOW_TEXTURE.getSprite();
      }
      if (glowEffectSprite == null) {
         matrixStack.popPose();
         return;
      }

      RenderType glowLayer = LootBeamRenderLayers.groundGlowEffect(
            glowEffectSprite.atlasLocation(),
            itemConfig.glowEffectTexture.isColored(),
            itemConfig.useGlowGradient,
            itemConfig.glowCustomShader);
      float radius = itemConfig.glowEffectRadius;
      float glowEffectAlpha = fadeAlpha * itemConfig.glowEffectAlpha;
      if (itemConfig.pulseGlow) {
         float pulseSpeed = itemConfig.pulseGlowSpeed / 10.0F;
         float cosineFactor = (float) Math.cos(currentGroundTime * pulseSpeed);
         float normalizedCosine = cosineFactor * 0.5F + 0.5F;
         glowEffectAlpha = (float) NumberHelper.clampedMapRange(
               normalizedCosine, 0, 1, itemConfig.pulseGlowMinAlpha, itemConfig.pulseGlowMaxAlpha) * fadeAlpha;
         radius = (float) NumberHelper.clampedMapRange(
               normalizedCosine, 0, 1, itemConfig.pulseGlowMinRadius, itemConfig.pulseGlowMaxRadius);
      }

      radius *= sizeMultiplier;
      if (radius < itemConfig.pulseGlowMinRadius) {
         radius = itemConfig.pulseGlowMinRadius;
      }

      float glowR = effectColor.fR;
      float glowG = effectColor.fG;
      float glowB = effectColor.fB;
      if (itemConfig.glowEffectTexture.isColored()) {
         float averageColor = LootBeamShaders.getAverageColor(LootBeamShaders.Shader.GLOW_OVERLAY);
         glowR = averageColor;
         glowG = averageColor;
         glowB = averageColor;
      }

      if (itemConfig.smoothGlowEffectRadius) {
         radius = NumberHelper.smoothValue(radius, currentGroundTime, itemConfig.smoothDuration);
      }
      if (itemConfig.smoothGlowEffectAlpha) {
         glowEffectAlpha = NumberHelper.smoothValue(glowEffectAlpha, currentGroundTime, itemConfig.smoothDuration);
      }

      boolean useGlowGradient = !itemConfig.glowEffectTexture.isColored() && itemConfig.useGlowGradient
            || itemConfig.glowCustomShader != LootBeamShaders.CustomShader.NONE;
      renderGlow(
            matrixStack,
            buffer.getBuffer(glowLayer),
            glowR,
            glowG,
            glowB,
            glowEffectAlpha,
            radius,
            glowEffectSprite,
            useGlowGradient,
            itemConfig.glowGradientModifiers,
            itemConfig.glowGradientStart / 100.0F,
            itemConfig.glowGradientEnd / 100.0F);
      matrixStack.popPose();
   }

   private static LootbeamsBufferBuilder addGlowVertex(
         VertexConsumer builder, Pose matrixEntry, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v) {
      Matrix4f poseMatrix = matrixEntry.pose();
      return (LootbeamsBufferBuilder) builder.addVertex(poseMatrix, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(matrixEntry, 0.0F, 1.0F, 0.0F);
   }

   private static void modifyGlowVertex(
         LootbeamsBufferBuilder builder,
         ColorHelper.Color color1,
         float centerU,
         float centerV,
         float uvWidth,
         float uvHeight,
         float gradientStart,
         float gradientEnd,
         boolean useGlowGradient) {
      if (useGlowGradient) {
         builder.uvCenter(centerU, centerV)
               .uvSize(uvWidth, uvHeight)
               .shortCustomData(gradientStart, gradientEnd)
               .color1(color1.R, color1.G, color1.B, color1.A);
      }
   }

   private static void renderGlow(
         PoseStack stack,
         VertexConsumer builder,
         float red,
         float green,
         float blue,
         float alpha,
         float radius,
         TextureAtlasSprite glowSprite,
         boolean useGlowGradient,
         List<String> gradientModifiers,
         float gradientStart,
         float gradientEnd) {
      Pose matrixentry = stack.last();
      boolean shadersLoaded = IrisCompat.isShaderPackInUse();
      float minX = glowSprite.getU0();
      float maxX = glowSprite.getU1();
      float minY = glowSprite.getV0();
      float maxY = glowSprite.getV1();
      float centerU = (minX + maxX) / 2.0F;
      float centerV = (minY + maxY) / 2.0F;
      float uvWidth = maxX - minX;
      float uvHeight = maxY - minY;
      ColorHelper.Color secondColor = ColorHelper.Color.of(DroplightRenderer.getSecondColor(red, green, blue, alpha, gradientModifiers));
      modifyGlowVertex(
            addGlowVertex(builder, matrixentry, -radius, 0.0F, -radius, red, green, blue, alpha, minX, minY),
            secondColor, centerU, centerV, uvWidth, uvHeight, gradientStart, gradientEnd, useGlowGradient && !shadersLoaded);
      modifyGlowVertex(
            addGlowVertex(builder, matrixentry, -radius, 0.0F, radius, red, green, blue, alpha, minX, maxY),
            secondColor, centerU, centerV, uvWidth, uvHeight, gradientStart, gradientEnd, useGlowGradient && !shadersLoaded);
      modifyGlowVertex(
            addGlowVertex(builder, matrixentry, radius, 0.0F, radius, red, green, blue, alpha, maxX, maxY),
            secondColor, centerU, centerV, uvWidth, uvHeight, gradientStart, gradientEnd, useGlowGradient && !shadersLoaded);
      modifyGlowVertex(
            addGlowVertex(builder, matrixentry, radius, 0.0F, -radius, red, green, blue, alpha, maxX, minY),
            secondColor, centerU, centerV, uvWidth, uvHeight, gradientStart, gradientEnd, useGlowGradient && !shadersLoaded);
   }
}
