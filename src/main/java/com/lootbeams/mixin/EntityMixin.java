package com.lootbeams.mixin;

import com.lootbeams.config.Configuration;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.helpers.TextColorHelper;
import com.lootbeams.helpers.ViewHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
   public EntityMixin() {
   }

   @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
   private void onHasOutline(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof ItemEntity itemEntity) {
         ItemStack itemStack = itemEntity.getItem();
         Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
         if (itemConfig.itemsGlow && ViewHelper.shouldRenderOnItem(itemStack)) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
   private void getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof ItemEntity itemEntity) {
         ItemStack itemStack = itemEntity.getItem();
         Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
         if (itemConfig.itemsGlow && ViewHelper.shouldRenderOnItem(itemStack)) {
            TextColor textColor = TextColorHelper.getItemColor(itemStack);
            if (textColor != null) {
               cir.setReturnValue(textColor.getValue());
            }
         }
      }
   }
}
