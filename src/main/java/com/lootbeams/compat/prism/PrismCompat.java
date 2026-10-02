package com.lootbeams.compat.prism;

import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.TextColor;

public class PrismCompat {
   public PrismCompat() {
   }

   public static boolean isPrismLoaded() {
      return FabricLoader.getInstance().isModLoaded("prism");
   }

   public static TextColor parseColor(Object value) {
      try {
         return (TextColor)Class.forName("com.anthonyhilyard.prism.util.ConfigHelper").getMethod("parseColor", Object.class).invoke(null, value);
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public static TextColor applyModifiers(List<String> modifiers, TextColor value) {
      try {
         return (TextColor)Class.forName("com.anthonyhilyard.prism.util.ConfigHelper")
            .getMethod("applyModifiers", List.class, TextColor.class)
            .invoke(null, modifiers, value);
      } catch (Exception var3) {
         var3.printStackTrace();
         return null;
      }
   }
}
