package com.lootbeams.managers;

import com.lootbeams.config.Configuration;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.helpers.ItemHelper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class TooltipManager {
   public static final Map<ItemStack, List<Component>> TOOLTIP_CACHE = new ConcurrentHashMap<>();

   public TooltipManager() {
   }

   public static void onResourcesReload() {
      TOOLTIP_CACHE.clear();
   }

   public static void onEntityRender(ItemStack itemStack) {
      // getTooltipFromCache builds it when a tag first needs it; building every item's tooltip every frame is costly.
      if (itemStack == null || itemStack.isEmpty() || fr.poubone.att2.client.hud.HUDConfig.get().lightItemChecks) {
         return;
      }
      if (!TOOLTIP_CACHE.containsKey(itemStack)) {
         TOOLTIP_CACHE.put(itemStack, getItemStackTooltip(itemStack));
      }
   }

   public static void onEntityRenderEnd(ItemStack itemStack) {
      if (itemStack == null) {
         return;
      }
      TOOLTIP_CACHE.remove(itemStack);
   }

   public static List<Component> getTooltipFromCache(ItemStack itemStack) {
      if (!TOOLTIP_CACHE.containsKey(itemStack)) {
         List<Component> tooltip = getItemStackTooltip(itemStack);
         TOOLTIP_CACHE.put(itemStack, tooltip);
         return tooltip;
      } else {
         return TOOLTIP_CACHE.get(itemStack);
      }
   }

   public static List<Component> getItemStackTooltip(ItemStack itemStack) {
      return Screen.getTooltipFromItem(Minecraft.getInstance(), itemStack);
   }

   public static Component getTooltipLongestLine(ItemStack itemStack, List<Component> tooltipLines) {
      Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
      Minecraft client = Minecraft.getInstance();
      Font textRenderer = client.font;
      Component longestLine = tooltipLines.get(0);
      if (itemConfig.screenTooltipsRequireCrouch && client.player.isCrouching() || ItemHelper.isBundle(itemStack)) {
         longestLine = tooltipLines.stream().max((a, b) -> textRenderer.width(a) - textRenderer.width(b)).orElse(tooltipLines.get(0));
      }

      return longestLine;
   }

   public static int[] getTooltipSize(List<Component> tooltipLines, Optional<TooltipComponent> tooltipData, Font textRenderer) {
      List<ClientTooltipComponent> components = tooltipLines.stream()
         .map(Component::getVisualOrderText)
         .map(ClientTooltipComponent::create)
         .collect(Util.toMutableList());
      tooltipData.ifPresent(componentData -> components.add(components.isEmpty() ? 0 : 1, ClientTooltipComponent.create(componentData)));
      int width = 0;
      int height = components.size() == 1 ? -2 : 0;
      if (!components.isEmpty()) {
         for (ClientTooltipComponent tooltipComponent : components) {
            int componentWidth = tooltipComponent.getWidth(textRenderer);
            if (componentWidth > width) {
               width = componentWidth;
            }

            height += tooltipComponent.getHeight(textRenderer);
         }
      }

      return new int[]{width, height};
   }

   public static List<Component> getTooltipWithStackSize(ItemStack itemStack) {
      List<Component> tooltipLines = getTooltipFromCache(itemStack);
      Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
      int count = itemStack.getCount();
      if (itemConfig.renderStackcount && fr.poubone.att2.client.hud.HUDConfig.get().renderStackcount && count > 1) {
         String countString = " x" + count;
         Component firstLine = tooltipLines.get(0);
         String itemNameString = firstLine.getString();
         if (!itemNameString.contains(countString)) {
            Component newLineWithCount = Component.literal(itemNameString + countString).setStyle(firstLine.getStyle());
            tooltipLines.set(0, newLineWithCount);
         }
      }

      return tooltipLines;
   }

   public static boolean canRenderTooltips(ItemStack itemStack) {
      Minecraft client = Minecraft.getInstance();
      boolean isChatScreen = client.screen instanceof ChatScreen;
      boolean inGame = client.screen == null;
      Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);
      return itemConfig.advancedTooltips && (inGame || isChatScreen);
   }

   public static class OffsetContainer {
      private final int height;
      private final int offset;
      private final int halfTooltipWidth;

      public OffsetContainer(int offset, int halfTooltipWidth, int height) {
         this.height = height;
         this.offset = offset;
         this.halfTooltipWidth = halfTooltipWidth;
      }

      public int getOffset() {
         return this.offset;
      }

      public int getHalfTooltipWidth() {
         return this.halfTooltipWidth;
      }

      public int getTooltipWidth() {
         return this.halfTooltipWidth * 2;
      }

      public int getTooltipHeight() {
         return this.height;
      }
   }
}
