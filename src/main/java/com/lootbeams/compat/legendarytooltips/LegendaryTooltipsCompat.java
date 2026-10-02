package com.lootbeams.compat.legendarytooltips;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

public class LegendaryTooltipsCompat {
   public LegendaryTooltipsCompat() {
   }

   public static boolean isLegendaryTooltipsLoaded() {
      return FabricLoader.getInstance().isModLoaded("legendarytooltips");
   }

   public static boolean showModelForItem(ItemStack itemStack) {
      try {
         return (Boolean)Class.forName("com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig")
            .getMethod("showModelForItem", ItemStack.class)
            .invoke(null, itemStack);
      } catch (Exception var2) {
         var2.printStackTrace();
         return false;
      }
   }
}
