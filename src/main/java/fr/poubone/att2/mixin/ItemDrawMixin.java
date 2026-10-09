package fr.poubone.att2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.poubone.att2.client.renderer.ItemFaceCulling;
import fr.poubone.att2.client.renderer.ItemOutlines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Both item draws of a submit (the item and its outline) get only the faces turned toward the camera, and the outline
 * draw goes straight to the outline buffer.
 */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemDrawMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"))
    private void att2$visibleFacesOnly(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                                       int[] tints, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foil,
                                       Operation<Void> original) {
        List<BakedQuad> visible = ItemFaceCulling.visible(context, poseStack.last(), quads, renderType);
        if (buffers instanceof OutlineBufferSource outline && ItemOutlines.draw(outline, context, poseStack.last(), visible, renderType)) {
            return;
        }
        original.call(context, poseStack, buffers, light, overlay, tints, visible, renderType, foil);
    }
}
