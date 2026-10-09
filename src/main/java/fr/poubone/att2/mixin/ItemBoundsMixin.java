package fr.poubone.att2.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemBounds;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Model bounds from {@link ItemBounds} instead of every point of the model each frame. */
@Mixin(ItemStackRenderState.class)
public abstract class ItemBoundsMixin {
    @Shadow
    ItemDisplayContext displayContext;
    @Shadow
    private int activeLayerCount;
    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;
    @Shadow
    private AABB cachedModelBoundingBox;

    @Inject(method = "getModelBoundingBox", at = @At("HEAD"), cancellable = true)
    private void att2$rememberedBounds(CallbackInfoReturnable<AABB> cir) {
        if (cachedModelBoundingBox != null || !HUDConfig.get().cacheItemBounds || !RenderSystem.isOnRenderThread()) return;
        AABB box = ItemBounds.of(layers, activeLayerCount, displayContext.leftHand());
        if (box != null) {
            cachedModelBoundingBox = box;
            cir.setReturnValue(box);
        }
    }
}
