package fr.poubone.att2.mixin;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD config {@code solidItemEntities}: dropped items drawn as cutout instead of translucent, which skips sorting
 * every quad of every dropped item each frame. Only the layers that use the two translucent item sheets change.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class SolidItemEntityMixin {
    @Unique
    private static RenderType att2$cutoutItemSheet;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            at = @At("RETURN"))
    private void att2$solidLayers(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (!HUDConfig.get().solidItemEntities) return;
        ItemStackRenderStateAccessor item = (ItemStackRenderStateAccessor) state.item;
        ItemStackRenderState.LayerRenderState[] layers = item.att2$layers();
        for (int i = 0; i < item.att2$activeLayerCount(); i++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) layers[i];
            RenderType type = layer.att2$renderType();
            if (type == Sheets.translucentItemSheet()) {
                if (att2$cutoutItemSheet == null) att2$cutoutItemSheet = RenderTypes.entityCutout(TextureAtlas.LOCATION_ITEMS);
                layers[i].setRenderType(att2$cutoutItemSheet);
            } else if (type == Sheets.translucentBlockItemSheet()) {
                layers[i].setRenderType(Sheets.cutoutBlockSheet());
            }
        }
    }
}
