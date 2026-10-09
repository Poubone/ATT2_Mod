package com.lootbeams.helpers;

import com.google.common.collect.Lists;
import com.lootbeams.LootBeams;
import com.lootbeams.compat.prism.PrismCompat;
import com.lootbeams.config.Configuration;
import com.lootbeams.features.CustomLootBeamsConfig;
import com.lootbeams.features.CustomRarity;
import com.lootbeams.managers.CrashManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class TextColorHelper {
   private static Map<ItemStack, TextColor> DC_MAP = new HashMap<>();
   private static String NBT_COLOR_KEY = "lootbeams.color";
   private static String NBT_ANIMATED_COLOR_KEY = "lootbeams.animated_color";

   public TextColorHelper() {
   }

   public static TextColor getItemColor(ItemStack itemStack) {
      if (CrashManager.CRASH_BLACKLIST.contains(itemStack)) {
         return TextColor.fromLegacyFormat(ChatFormatting.WHITE);
      } else {
         Configuration itemConfig = CustomLootBeamsConfig.fromItemStack(itemStack);

         try {
            TextColor override = getColorFromItemOverrides(itemStack.getItem());
            if (override != null) {
               return override;
            } else {
               if (itemStack.getComponents().has(DataComponents.CUSTOM_DATA)) {
                  CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
                  if (data != null) {
                     CompoundTag value = fr.poubone.att2.client.data.CustomDataView.read(data);
                     if (value.contains(NBT_ANIMATED_COLOR_KEY) && PrismCompat.isPrismLoaded()) {
                        if (DC_MAP.containsKey(itemStack)) {
                           return TextColor.fromRgb(DC_MAP.get(itemStack).getValue());
                        }

                        TextColor dColor = PrismCompat.parseColor(value.getStringOr(NBT_ANIMATED_COLOR_KEY, ""));
                        DC_MAP.put(itemStack, dColor);
                        return TextColor.fromRgb(dColor.getValue());
                     }

                     if (value.contains(NBT_COLOR_KEY)) {
                        return TextColor.parseColor(value.getStringOr(NBT_COLOR_KEY, "#FFFFFF")).getOrThrow();
                     }
                  }
               }

               if (itemConfig.renderRarityColor) {
                  CustomRarity customRarity = CustomRarity.fromItemStack(itemStack);
                  if (customRarity != null) {
                     return TextColor.fromRgb(customRarity.getColor());
                  }
               }

               fr.poubone.att2.client.renderer.ItemRarity att2Rarity = fr.poubone.att2.client.renderer.ItemRarity.fromStack(itemStack);
               if (att2Rarity != null) {
                  return TextColor.fromRgb(att2Rarity.rgb);
               }

               if (itemConfig.renderNameColor) {
                  TextColor nameColor = getRawColor(itemStack.getHoverName());
                  if (!nameColor.equals(TextColor.fromLegacyFormat(ChatFormatting.WHITE))) {
                     return nameColor;
                  }
               }

               if (itemConfig.renderRarityColor) {

                  if (itemStack.getRarity().color() != null) {
                     return TextColor.fromLegacyFormat(itemStack.getRarity().color());
                  }
               }

               return TextColor.fromLegacyFormat(ChatFormatting.WHITE);
            }
         } catch (Exception var6) {
            CrashManager.LOGGER.error("Failed to get color for ({}), added to temporary blacklist", itemStack.getDisplayName());
            CrashManager.CRASH_BLACKLIST.add(itemStack);
            CrashManager.LOGGER.info("Temporary blacklist is now : ");

            for (ItemStack s : CrashManager.CRASH_BLACKLIST) {
               CrashManager.LOGGER.info(s.getDisplayName());
            }

            return TextColor.fromLegacyFormat(ChatFormatting.WHITE);
         }
      }
   }

   public static TextColor getColorFromItemOverrides(Item i) {
      List<String> overrides = LootBeams.config.colorOverrides;
      if (overrides.isEmpty()) {
         return null;
      } else {
         for (String unparsed : overrides.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList())) {
            String[] configValue = unparsed.split("=");
            if (configValue.length == 2) {
               String nameIn = configValue[0];
               Identifier registry = Identifier.tryParse(nameIn.replace("#", ""));
               TextColor colorIn = null;

               try {
                  if (PrismCompat.isPrismLoaded()) {
                     if (DC_MAP.containsKey(i.getDefaultInstance())) {
                        colorIn = TextColor.fromRgb(DC_MAP.get(i.getDefaultInstance()).getValue());
                     } else {
                        TextColor dColor = PrismCompat.parseColor(configValue[1]);
                        DC_MAP.put(i.getDefaultInstance(), dColor);
                        colorIn = TextColor.fromRgb(dColor.getValue());
                     }
                  } else {
                     colorIn = (TextColor)TextColor.parseColor(configValue[1]).getOrThrow();
                  }
               } catch (Exception var9) {
                  CrashManager.LOGGER.error(String.format("Color overrides error! \"%s\" is not a valid hex color for \"%s\"", configValue[1], nameIn));
                  return null;
               }

               if (!nameIn.contains(":") && BuiltInRegistries.ITEM.getKey(i).getNamespace().equals(nameIn)) {
                  return colorIn;
               }

               if (registry != null) {
                  if (nameIn.startsWith("#")) {
                     Optional<Named<Item>> tag = BuiltInRegistries.ITEM.getTags().filter(pair -> pair.key().location().equals(registry)).findFirst();
                     if (tag.isPresent()
                        && tag.get().contains((Holder)BuiltInRegistries.ITEM.get((ResourceKey)BuiltInRegistries.ITEM.getResourceKey(i).get()).get())) {
                        return colorIn;
                     }
                  }

                  Optional<Item> registryItem = BuiltInRegistries.ITEM.getOptional(registry);
                  if (registryItem.isPresent() && registryItem.get().asItem() == i.asItem()) {
                     return colorIn;
                  }
               }
            }
         }

         return null;
      }
   }

   public static TextColor getRawColor(Component text) {
      List<Style> list = Lists.newArrayList();
      text.visit((acceptor, styleIn) -> {
         StringDecomposer.iterateFormatted(styleIn, acceptor, (string, style, consumer) -> {
            list.add(style);
            return true;
         });
         return Optional.empty();
      }, Style.EMPTY);
      return !list.isEmpty() && list.get(0).getColor() != null ? list.get(0).getColor() : TextColor.fromLegacyFormat(ChatFormatting.WHITE);
   }

   private static String compensateHex(String hex) {
      StringBuilder compensatedHex = new StringBuilder();

      for (char c : hex.toCharArray()) {
         compensatedHex.append(c).append(c);
      }

      return compensatedHex.toString();
   }

   public static int getColorFromHEX(String hex) {
      int defaultColor = Integer.parseInt("FFFFFF", 16);
      if (hex != null && !hex.isEmpty()) {
         if (hex.contains("#")) {
            hex = hex.replace("#", "");
         }

         if (!hex.isEmpty()) {
            switch (hex.length()) {
               case 3:
                  return Integer.parseInt(compensateHex(hex), 16);
               case 4:
                  return Integer.parseInt(compensateHex(hex).substring(0, 6), 16);
               case 5:
               case 7:
               default:
                  break;
               case 6:
                  return Integer.parseInt(hex, 16);
               case 8:
                  return Integer.parseInt(hex.substring(0, 6), 16);
            }
         }

         return defaultColor;
      } else {
         return defaultColor;
      }
   }
}
