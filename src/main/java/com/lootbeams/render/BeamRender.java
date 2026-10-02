package com.lootbeams.render;

import com.lootbeams.config.Configuration;
import com.lootbeams.features.BeamOpacityOnApproach;
import com.lootbeams.features.BeamSizeOnApproach;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.helpers.NumberHelper;
import com.lootbeams.helpers.TextColorHelper;
import com.lootbeams.managers.CrashManager;
import com.lootbeams.renderers.DroplightRenderer;
import com.lootbeams.renderers.GroundEffectRenderer;
import com.lootbeams.renderers.IBeamRenderer;
import com.lootbeams.renderers.LootBeamRenderer;
import com.lootbeams.renderers.NameTagRenderer;
import com.lootbeams.utils.ParticleEmitter;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.item.ItemEntity;

public class BeamRender {
   private static final Map<ItemEntity, Integer> ITEM_GROUND_START_TIMES = new HashMap<>();
   private static boolean loggedRenderer;

   public BeamRender() {
   }

   static float fadeDistanceAlpha(float distance, Configuration itemConfig) {
      float defaultMultiplier = 1.0F;
      if (itemConfig.beamOpacityOnApproach == BeamOpacityOnApproach.DISABLED) {
         return defaultMultiplier;
      }

      float renderDistance = itemConfig.renderDistance;
      float changeDistance = itemConfig.changeDistance;
      float changeOffset = itemConfig.changeOffset;
      if (renderDistance <= 24.0F || changeDistance < renderDistance || changeOffset < 8.0F) {
         renderDistance = Math.max(renderDistance, 48.0F);
         changeDistance = renderDistance;
         changeOffset = Math.max(renderDistance - 8.0F, 0.0F);
      }

      float fadeEnd = Math.max(changeDistance, renderDistance);
      float fadeStart = Math.min(changeOffset, fadeEnd);
      if (fadeStart < fadeEnd - 8.0F && (fadeStart < 8.0F || fadeStart * 4.0F < fadeEnd)) {
         fadeStart = Math.max(fadeEnd - 8.0F, 0.0F);
      }

      if (itemConfig.beamOpacityOnApproach == BeamOpacityOnApproach.FADE_IN) {
         return (float)NumberHelper.clampedMapRange(distance, fadeStart, fadeEnd, 1.0F, 0.0F);
      } else {
         return itemConfig.beamOpacityOnApproach == BeamOpacityOnApproach.FADE_OUT
            ? (float)NumberHelper.clampedMapRange(distance, fadeStart, fadeEnd, 0.0F, 1.0F)
            : defaultMultiplier;
      }
   }

   private static float getBeamSizeMultiplier(float distance, Configuration itemConfig) {
      float defaultMultiplier = 1.0F;
      if (itemConfig.beamSizeOnApproach == BeamSizeOnApproach.DISABLED) {
         return defaultMultiplier;
      } else if (itemConfig.beamSizeOnApproach == BeamSizeOnApproach.SHRINK) {
         return (float)NumberHelper.clampedMapRange(distance, itemConfig.changeOffset, itemConfig.changeDistance, 0.0F, 1.0F);
      } else {
         return itemConfig.beamSizeOnApproach == BeamSizeOnApproach.GROW
            ? (float)NumberHelper.clampedMapRange(distance, itemConfig.changeOffset, itemConfig.changeDistance, 1.0F, 0.0F)
            : defaultMultiplier;
      }
   }

   public static boolean canRenderBeam(Configuration itemConfig, float fadeAlpha) {
      return !(itemConfig.beamAlpha * fadeAlpha < 0.01F);
   }

   public static void render(PoseStack stack, BufferSource buffer, ItemEntity item, long worldtime, float pticks) {
      if (!ITEM_GROUND_START_TIMES.containsKey(item)) {
         ITEM_GROUND_START_TIMES.put(item, item.getAge());
      }

      float itemGroundStartTime = ITEM_GROUND_START_TIMES.getOrDefault(item, 0).intValue();
      float currentGroundTime = item.getAge() - itemGroundStartTime + pticks;
      Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(item.getItem());
      LocalPlayer player = Minecraft.getInstance().player;
      float distance = player != null ? player.distanceTo(item) : 0.0F;
      float sizeMultiplier = getBeamSizeMultiplier(distance, itemConfig);
      float fadeAlpha = fadeDistanceAlpha(distance, itemConfig);
      if (canRenderBeam(itemConfig, fadeAlpha)) {
         TextColor color = TextColorHelper.getItemColor(item.getItem());
         if (item.onGround()) {
            stack.pushPose();
            GroundEffectRenderer.renderGroundEffect(buffer, stack, item, itemConfig, color, sizeMultiplier, fadeAlpha, currentGroundTime, worldtime, pticks);
            if (!loggedRenderer) {
            loggedRenderer = true;
            CrashManager.LOGGER.info(
                  "Loot Beams renderer: {} (droplight={}, radius={}, height={})",
                  itemConfig.renderDroplightBeam ? "Droplight" : "Classic",
                  itemConfig.renderDroplightBeam,
                  itemConfig.beamRadius,
                  itemConfig.beamHeight);
         }
         IBeamRenderer BeamRenderer = itemConfig.renderDroplightBeam ? DroplightRenderer::renderBeam : LootBeamRenderer::renderBeam;
            BeamRenderer.renderBeam(buffer, stack, item, itemConfig, color, sizeMultiplier, fadeAlpha, currentGroundTime, worldtime, pticks);
            stack.popPose();
            NameTagRenderer.renderNameTags(buffer, stack, item, itemConfig, color, fadeAlpha, currentGroundTime, worldtime, pticks);
            ParticleEmitter.createParticlesForItem(item, itemConfig, item.getAge(), color, fadeAlpha, pticks);
         }

         ITEM_GROUND_START_TIMES.keySet().removeIf(itemEntity -> !itemEntity.onGround());
      }
   }
}
