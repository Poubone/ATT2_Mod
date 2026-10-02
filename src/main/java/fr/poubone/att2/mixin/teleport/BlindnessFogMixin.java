package fr.poubone.att2.mixin.teleport;

import fr.poubone.att2.client.teleport.TeleportTransitionController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The map gives one second of Blindness when the player lands on a Teleportation Array. That black fog would
 * cut the end of the cinematic, so it is ignored for the local player while the transition runs and shortly after.
 */
@Mixin(MobEffectFogEnvironment.class)
public abstract class BlindnessFogMixin {
    @Shadow
    public abstract Holder<MobEffect> getMobEffect();

    @Inject(method = "isApplicable", at = @At("HEAD"), cancellable = true)
    private void att2$skipBlindnessDuringTransition(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == Minecraft.getInstance().player
                && this.getMobEffect().is(MobEffects.BLINDNESS)
                && TeleportTransitionController.shouldSuppressArrivalEffects()) {
            cir.setReturnValue(false);
        }
    }
}
