package com.lootbeams.helpers;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class TextHelper {
   public TextHelper() {
   }

   public static Component centeredLine(Component text, Font textRenderer, int width) {
      String centeredText = text.getString();

      while (textRenderer.width(centeredText) < width) {
         centeredText = " " + centeredText + " ";
      }

      return Component.literal(centeredText).setStyle(text.getStyle());
   }
}
