package com.lootbeams.mixin;

import com.lootbeams.contexts.WorldRendererContext;
import com.lootbeams.events.RenderEvents;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;
   @Shadow
   private ClientLevel level;

   @Unique
   private final WorldRendererContext lootbeams$context = new WorldRendererContext();

   @Inject(method = "renderLevel", at = @At("HEAD"))
   private void lootbeams$beforeRender(
         GraphicsResourceAllocator allocator,
         DeltaTracker tickCounter,
         boolean renderBlockOutline,
         Camera camera,
         Matrix4f positionMatrix,
         Matrix4f projectionMatrix,
         Matrix4f cullMatrix,
         GpuBufferSlice fog,
         Vector4f fogColor,
         boolean renderSky,
         CallbackInfo ci) {
      this.lootbeams$context.prepare(
            (LevelRenderer) (Object) this,
            tickCounter,
            renderBlockOutline,
            camera,
            this.minecraft.gameRenderer,
            projectionMatrix,
            positionMatrix,
            this.renderBuffers.bufferSource(),
            Minecraft.useShaderTransparency(),
            this.level);
      if (this.lootbeams$context.getMatrixStack() == null) {
         this.lootbeams$context.setMatrixStack(new PoseStack());
      }
   }

   @ModifyArg(
         method = "addParticlesPass",
         at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"),
         index = 0)
   private Runnable lootbeams$beforeParticles(Runnable original) {
      return () -> {
         this.lootbeams$preparePass();
         RenderEvents.BEFORE_PARTICLES.invoker().beforeParticles(this.lootbeams$context);
         original.run();
      };
   }

   @ModifyArg(
         method = "addMainPass",
         at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"),
         index = 0)
   private Runnable lootbeams$afterTranslucent(Runnable original) {
      return () -> {
         original.run();
         this.lootbeams$preparePass();
         RenderEvents.AFTER_TRANSLUCENT.invoker().afterTranslucent(this.lootbeams$context);
      };
   }

   @ModifyArg(
         method = "addWeatherPass",
         at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"),
         index = 0)
   private Runnable lootbeams$afterWeather(Runnable original) {
      return () -> {
         original.run();
         this.lootbeams$preparePass();
         RenderEvents.AFTER_WEATHER.invoker().afterWeather(this.lootbeams$context);
      };
   }

   @ModifyArg(
         method = "addLateDebugPass",
         at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"),
         index = 0)
   private Runnable lootbeams$beforeEnd(Runnable original) {
      return () -> {
         original.run();
         this.lootbeams$preparePass();
         RenderEvents.BEFORE_END.invoker().beforeEnd(this.lootbeams$context);
      };
   }

   @Inject(method = "renderLevel", at = @At("RETURN"))
   private void lootbeams$afterRender(CallbackInfo ci) {
      RenderEvents.END.invoker().onEnd(this.lootbeams$context);
   }

   @Unique
   private void lootbeams$preparePass() {
      this.lootbeams$context.setConsumers(this.renderBuffers.bufferSource());
      this.lootbeams$context.setCamera(this.minecraft.gameRenderer.getMainCamera());
      this.lootbeams$context.setTickCounter(this.minecraft.getDeltaTracker());
      this.lootbeams$context.setWorld(this.level);
      if (this.lootbeams$context.getMatrixStack() == null) {
         this.lootbeams$context.setMatrixStack(new PoseStack());
      }
   }
}
