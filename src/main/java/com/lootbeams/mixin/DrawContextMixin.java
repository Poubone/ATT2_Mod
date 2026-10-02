package com.lootbeams.mixin;

import com.lootbeams.managers.TooltipManager;
import com.lootbeams.renderers.TooltipRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class DrawContextMixin {
   @Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
   private void lootbeams$drawItemTooltip(Font font, ItemStack itemStack, int x, int y, CallbackInfo ci) {
      // Chat SHOW_ITEM hovers must use the inventory tooltip, including TOOLTIP_STYLE
      // and the full lore, not the abbreviated/repositioned world-loot tooltip.
      if (Minecraft.getInstance().screen instanceof ChatScreen) return;
      if (TooltipManager.canRenderTooltips(itemStack)) {
         TooltipRenderer.renderItemTooltip((GuiGraphics) (Object) this, font, itemStack, x, y);
         ci.cancel();
      }
   }
}
