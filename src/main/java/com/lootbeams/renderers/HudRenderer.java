package com.lootbeams.renderers;

import com.lootbeams.config.Configuration;
import com.lootbeams.containers.EntityRenderStateContainer;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.helpers.TargetHelper;
import com.lootbeams.helpers.ViewHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class HudRenderer {
   public HudRenderer() {
   }

   public static void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
      float tickDelta = tickCounter.getGameTimeDeltaPartialTick(false);
      Minecraft client = Minecraft.getInstance();
      if (ViewHelper.shouldRenderCrosshair(client)) {
         if (client.hitResult.getType() == Type.ENTITY) {
            Entity entity = ((EntityHitResult)client.hitResult).getEntity();
            if (entity instanceof ItemFrame itemFrameEntity) {
               ItemFrameRenderState renderState = (ItemFrameRenderState)EntityRenderStateContainer.getRenderState(entity);
               ItemStack itemStack = itemFrameEntity.getItem();
               Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
               if (renderState != null && renderState.nameTagAttachment != null && itemConfig.itemFrameTooltips) {
                  TooltipRenderer.renderWorldPositionTooltip(drawContext, entity, itemStack, tickDelta);
                  return;
               }

               return;
            }
         }

         Player player = client.player;
         HitResult result = TargetHelper.getEntityItem(player);
         if (result != null && result.getType() == Type.ENTITY) {
            Entity entity = ((EntityHitResult)result).getEntity();
            if (entity instanceof ItemEntity itemEntity) {
               ItemStack itemStack = itemEntity.getItem();
               Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
               boolean shouldRender = ViewHelper.shouldRenderOnItem(itemStack);
               if (!shouldRender || itemConfig.requireOnGround && !entity.onGround()) {
                  return;
               }

               TooltipRenderer.renderWorldPositionTooltip(drawContext, entity, itemStack, tickDelta);
            }
         }
      }
   }
}
