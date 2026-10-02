package com.lootbeams.mixin;

import com.lootbeams.config.Configuration;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.features.CustomRarity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
   @Unique
   private boolean isTooltipModificationActive = false;

   public ItemStackMixin() {
   }

   @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
   private void modifyTooltip(TooltipContext context, @Nullable Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
      ItemStack itemStack = (ItemStack) (Object) this;
      List<Component> tooltip = (List<Component>)cir.getReturnValue();
      if (!this.isTooltipModificationActive) {
         this.isTooltipModificationActive = true;

         try {
            CustomRarity customRarity = CustomRarity.fromItemStack(itemStack);
            Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
            if (itemConfig.renderItemRarity && itemConfig.renderItemRarityInTooltip && customRarity != null) {
               tooltip.add(CustomRarity.toText(customRarity));
            }
         } finally {
            this.isTooltipModificationActive = false;
         }
      }

      cir.setReturnValue(tooltip);
   }
}
