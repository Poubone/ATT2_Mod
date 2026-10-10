package fr.poubone.att2.mixin;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OutlineBufferSource.class)
public interface OutlineBufferSourceAccessor {
    @Accessor("outlineBufferSource")
    MultiBufferSource.BufferSource att2$buffers();

    @Accessor("outlineColor")
    int att2$color();
}
