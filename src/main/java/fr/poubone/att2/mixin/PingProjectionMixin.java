package fr.poubone.att2.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fr.poubone.att2.client.sync.PingMarkers;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use the actual world camera projection (dynamic FOV, third person and view bob included). */
@Mixin(LevelRenderer.class)
public class PingProjectionMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void att2$capturePingProjection(GraphicsResourceAllocator allocator, DeltaTracker ticks, boolean outline,
            Camera camera, Matrix4f view, Matrix4f projection, Matrix4f culling, GpuBufferSlice fog,
            Vector4f fogColor, boolean sky, CallbackInfo ci) {
        PingMarkers.captureMatrices(camera, view, projection, ticks.getGameTimeDeltaPartialTick(true));
    }
}
