package fr.poubone.att2.mixin.teleport;

import fr.poubone.att2.client.teleport.TeleportTransitionController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Companion of {@link BlindnessFogMixin}: keeps the sky visible while the arrival Blindness is muted. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSkyMixin {
    @Inject(method = "doesMobEffectBlockSky", at = @At("HEAD"), cancellable = true)
    private void att2$keepSkyDuringTransition(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        if (camera.entity() == Minecraft.getInstance().player && TeleportTransitionController.shouldSuppressArrivalEffects()) {
            cir.setReturnValue(false);
        }
    }
}
