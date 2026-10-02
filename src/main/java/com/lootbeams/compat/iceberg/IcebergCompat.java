package com.lootbeams.compat.iceberg;

import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class IcebergCompat {
   public IcebergCompat() {
   }

   public static boolean isIcebergLoaded() {
      return FabricLoader.getInstance().isModLoaded("iceberg");
   }

   public static String getPlatformName() {
      try {
         Object platformHelper = Class.forName("com.anthonyhilyard.iceberg.services.Services").getMethod("getPlatformHelper").invoke(null);
         return (String)Class.forName("com.anthonyhilyard.iceberg.services.IPlatformHelper").getMethod("getPlatformName").invoke(platformHelper);
      } catch (Exception var1) {
         var1.printStackTrace();
         return null;
      }
   }

   public static Rect2i getTooltipRect(
      ItemStack itemStack,
      GuiGraphics context,
      ClientTooltipPositioner positioner,
      List<ClientTooltipComponent> components,
      int mouseX,
      int mouseY,
      int screenWidth,
      int screenHeight,
      int maxTextWidth,
      Font textRenderer,
      int minWidth,
      boolean centeredTitle
   ) {
      try {
         return (Rect2i)Class.forName("com.anthonyhilyard.iceberg.util.Tooltips")
            .getMethod(
               "calculateRect",
               ItemStack.class,
               GuiGraphics.class,
               ClientTooltipPositioner.class,
               List.class,
               int.class,
               int.class,
               int.class,
               int.class,
               int.class,
               Font.class,
               int.class,
               boolean.class
            )
            .invoke(
               null, itemStack, context, positioner, components, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, textRenderer, minWidth, centeredTitle
            );
      } catch (Exception var13) {
         var13.printStackTrace();
         return null;
      }
   }

   public static List<ClientTooltipComponent> getTooltipComponents(
      ItemStack itemStack,
      List<? extends Component> textElements,
      Optional<TooltipComponent> tooltipData,
      int mouseX,
      int screenWidth,
      int screenHeight,
      Font forcedFont,
      Font fallbackFont,
      int maxWidth
   ) {
      try {
         return (List<ClientTooltipComponent>)Class.forName("com.anthonyhilyard.iceberg.util.Tooltips")
            .getMethod(
               "gatherTooltipComponents", ItemStack.class, List.class, Optional.class, int.class, int.class, int.class, Font.class, Font.class, int.class
            )
            .invoke(null, itemStack, textElements, tooltipData, mouseX, screenWidth, screenHeight, forcedFont, fallbackFont, maxWidth);
      } catch (Exception var10) {
         var10.printStackTrace();
         return null;
      }
   }
}
