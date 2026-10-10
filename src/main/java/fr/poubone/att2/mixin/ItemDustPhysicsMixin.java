package fr.poubone.att2.mixin;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemParticleBudget;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The map's dust above dropped items floats in the air; block collisions only cost particle ticking. */
@Mixin(ParticleEngine.class)
public abstract class ItemDustPhysicsMixin {
    @Inject(method = "createParticle", at = @At("RETURN"))
    private void att2$noCollisionsForItemDust(ParticleOptions options, double x, double y, double z,
                                              double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        Particle particle = cir.getReturnValue();
        if (particle != null && ItemParticleBudget.isSpawningItemDust() && HUDConfig.get().dustWithoutCollisions) {
            ((ParticleAccessor) particle).att2$setHasPhysics(false);
        }
    }
}
