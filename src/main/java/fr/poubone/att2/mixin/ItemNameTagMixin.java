package fr.poubone.att2.mixin;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemNameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A dropped item's name tag is the same object from frame to frame while it does not change; see {@link ItemNameTags}. */
@Mixin(EntityRenderer.class)
public abstract class ItemNameTagMixin {
    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void att2$cachedItemName(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof ItemEntity item && HUDConfig.get().cacheNameTags) {
            Component tag = ItemNameTags.cached(item);
            if (tag != null) cir.setReturnValue(tag);
        }
    }

    @Inject(method = "getNameTag", at = @At("RETURN"))
    private void att2$rememberItemName(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof ItemEntity item && HUDConfig.get().cacheNameTags && cir.getReturnValue() != null) {
            ItemNameTags.remember(item, cir.getReturnValue());
        }
    }
}
