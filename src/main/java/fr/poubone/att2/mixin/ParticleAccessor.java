package fr.poubone.att2.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("hasPhysics")
    void att2$setHasPhysics(boolean hasPhysics);
}
