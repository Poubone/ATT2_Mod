package com.lootbeams.mixin;

import com.lootbeams.config.Configuration;
import com.lootbeams.containers.EntityRenderStateContainer;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.helpers.ViewHelper;
import com.lootbeams.managers.BeamBudget;
import com.lootbeams.managers.ItemEntityManager;
import com.lootbeams.managers.RenderManager;
import com.lootbeams.managers.TooltipManager;
import com.lootbeams.render.BeamRender;
import com.lootbeams.render.LootBeamPerf;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Shadow
   @Final
   private EntityRenderDispatcher entityRenderDispatcher;

   @Shadow
   protected abstract boolean shouldShowName(T entity, double distanceToCameraSq);

   @Inject(method = "submit", at = @At("HEAD"))
   private void lootbeams$scheduleBeams(S state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      if (!(state instanceof ItemEntityRenderState)) {
         return;
      }
      ItemStack itemStack = ItemEntityManager.getItemStack(state);
      ItemEntity itemEntity = itemStack == null ? null : ItemEntityManager.getEntityForStack(itemStack);
      if (itemEntity == null) {
         return;
      }
      Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
      if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.distanceTo(itemEntity) > itemConfig.renderDistance) {
         return;
      }
      if (!ViewHelper.shouldRenderOnItem(itemEntity.getItem()) || !itemEntity.onGround()) {
         return;
      }
      BeamBudget.offer(itemEntity, state.distanceToCameraSq);
      RenderManager.addRenderAfterWeather((stack, consumer) -> {
         if (!BeamBudget.allows(itemEntity)) {
            stack.pushPose();
            stack.translate(itemEntity.position().x(), itemEntity.position().y(), itemEntity.position().z());
            BeamRender.renderNameTagOnly(stack, LootBeamPerf.buffers(), itemEntity, itemEntity.level().getGameTime(),
                  Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
            stack.popPose();
            return;
         }
         stack.pushPose();
         stack.translate(itemEntity.position().x(), itemEntity.position().y(), itemEntity.position().z());
         BeamRender.render(
               stack,
               LootBeamPerf.buffers(),
               itemEntity,
               itemEntity.level().getGameTime(),
               Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
         stack.popPose();
      });
   }

   /**
    * Items stay drawn out to the beam render distance, but still only when on screen: the culling box is
    * raised by the beam's height so a beam whose item is just below the view still shows.
    */
   @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
   private void lootbeams$extendItemRenderDistance(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof ItemEntity itemEntity) {
         Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemEntity.getItem());
         double maxDistance = itemConfig.renderDistance;
         if (this.entityRenderDispatcher.distanceToSqr(entity) < maxDistance * maxDistance) {
            if (!LootBeamPerf.culled()) {
               cir.setReturnValue(true); // the original drew every item in range, on screen or not
               return;
            }
            double beamTop = Math.max(itemConfig.beamHeight, BEAM_GLOW_HEIGHT) + itemConfig.beamYOffset + 1.0;
            cir.setReturnValue(frustum.isVisible(entity.getBoundingBox().inflate(0.5).expandTowards(0.0, beamTop, 0.0)));
         }
      }
   }

   /** The Droplight glow is drawn up to this height whatever the beam height. */
   @Unique
   private static final double BEAM_GLOW_HEIGHT = 2.5;

   @Inject(method = "submitNameTag", at = @At("HEAD"), cancellable = true)
   private void lootbeams$hideItemFrameLabels(S state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      if (state instanceof ItemFrameRenderState) {
         ItemStack itemStack = ItemEntityManager.getItemStack(state);
         if (itemStack == null) {
            return;
         }
         Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
         if (itemConfig.advancedTooltips && itemConfig.itemFrameTooltips) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "extractRenderState", at = @At("RETURN"))
   private void lootbeams$trackItemEntities(T entity, S state, float tickDelta, CallbackInfo ci) {
      if (state instanceof ItemEntityRenderState && entity instanceof ItemEntity itemEntity) {
         ItemStack itemStack = itemEntity.getItem();
         ItemEntityManager.track(itemStack, itemEntity);
         ItemEntityManager.track(state, itemStack);
         TooltipManager.onEntityRender(itemStack);
         if (lootbeams$shouldFullbright(itemEntity)) {
            state.lightCoords = 15728640;
         }
      }
      if (state instanceof ItemFrameRenderState && entity instanceof ItemFrame itemFrame) {
         ItemEntityManager.track(state, itemFrame.getItem());
      }

      boolean showName = state.distanceToCameraSq < 4096.0 && this.shouldShowName(entity, state.distanceToCameraSq);
      if (!showName) {
         state.nameTag = null;
         state.nameTagAttachment = null;
      }
      EntityRenderStateContainer.setRenderState(entity, state);
   }

   @Inject(method = "submit", at = @At("RETURN"))
   private void lootbeams$untrackItemEntities(S state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      if (state instanceof ItemEntityRenderState) {
         ItemStack itemStack = ItemEntityManager.getItemStack(state);
         // Pickup particles re-submit an ItemEntityRenderState without extractRenderState,
         // so there is no tracked stack. ConcurrentHashMap rejects a null key.
         if (itemStack != null) {
            ItemEntityManager.untrack(itemStack);
            TooltipManager.onEntityRenderEnd(itemStack);
         }
         ItemEntityManager.untrack(state);
      }
      if (state instanceof ItemFrameRenderState) {
         ItemEntityManager.untrack(state);
      }
   }

   @Unique
   private static boolean lootbeams$shouldFullbright(ItemEntity itemEntity) {
      return ViewHelper.shouldRenderOnItem(itemEntity.getItem());
   }
}
