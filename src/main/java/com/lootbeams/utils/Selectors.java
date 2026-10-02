package com.lootbeams.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag.Default;

public class Selectors {
   private static Map<String, Rarity> rarities = new HashMap<String, Rarity>() {
      {
         this.put("common", Rarity.COMMON);
         this.put("uncommon", Rarity.UNCOMMON);
         this.put("rare", Rarity.RARE);
         this.put("epic", Rarity.EPIC);
      }
   };
   private static Map<String, BiPredicate<Tag, String>> nbtComparators = new HashMap<>();

   public Selectors() {
   }

   public static boolean validateSelector(String value) {
      if (value.contains("+")) {
         for (String selector : value.split("\\+")) {
            if (!validateSelector(selector)) {
               return false;
            }
         }

         return true;
      } else if (value.startsWith("~")) {
         return validateSelector(value.substring(1));
      } else if (value.contentEquals("*")) {
         return true;
      } else if (value.startsWith("$")) {
         return Identifier.tryParse(value.substring(1)) != null;
      } else if (value.startsWith("@")) {
         return value.substring(1).matches("^[a-z][a-z0-9_-]{1,63}$");
      } else if (value.startsWith("!")) {
         return rarities.keySet().contains(value.substring(1).toLowerCase());
      } else if (value.startsWith("#")) {
         return TextColor.parseColor(value).result().orElse(null) != null;
      } else if (value.startsWith("%") || value.startsWith("^")) {
         return true;
      } else {
         return value.startsWith("&") ? true : value == null || value == "" || Identifier.tryParse(value) != null;
      }
   }

   public static boolean itemMatches(ItemStack item, String selector, Provider provider) {
      if (item.isEmpty()) {
         return false;
      } else if (selector.contains("+")) {
         for (String tooltipText : selector.split("\\+")) {
            if (!itemMatches(item, tooltipText, provider)) {
               return false;
            }
         }

         return true;
      } else if (selector.startsWith("~")) {
         return !itemMatches(item, selector.substring(1), provider);
      } else if (selector.contentEquals("*")) {
         return true;
      } else {
         String itemIdentifier = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
         if (!selector.equals(itemIdentifier) && !selector.equals(itemIdentifier.replace("minecraft:", ""))) {
            if (selector.startsWith("@")) {
               if (itemIdentifier.startsWith(selector.substring(1) + ":")) {
                  return true;
               }
            } else if (selector.startsWith("#")) {
               TextColor entryColor = (TextColor)TextColor.parseColor(selector).result().orElse(null);
               if (entryColor != null && entryColor.equals(getColorForItem(item, TextColor.fromRgb(16777215)))) {
                  return true;
               }
            } else if (selector.startsWith("!")) {
               if (item.getRarity() == rarities.get(selector.substring(1))) {
                  return true;
               }
            } else if (selector.startsWith("$")) {
               Optional<TagKey<Item>> matchingTag = BuiltInRegistries.ITEM
                  .listTagIds()
                  .filter(tagKey -> tagKey.location().equals(Identifier.parse(selector.substring(1))))
                  .findFirst();
               if (matchingTag.isPresent() && item.is(matchingTag.get())) {
                  return true;
               }
            } else if (selector.startsWith("%")) {
               if (item.getDisplayName().getString().contains(selector.substring(1))) {
                  return true;
               }
            } else if (selector.startsWith("^")) {
               Minecraft mc = Minecraft.getInstance();
               List<Component> lines = item.getTooltipLines(TooltipContext.EMPTY, mc.player, Default.ADVANCED);
               String tooltipTextx = "";

               for (int n = 1; n < lines.size(); n++) {
                  tooltipTextx = tooltipTextx + lines.get(n).getString() + "\n";
               }

               if (tooltipTextx.contains(selector.substring(1))) {
                  return true;
               }
            } else if (selector.startsWith("&")) {
               return false;
            }

            return false;
         } else {
            return true;
         }
      }
   }

   private static boolean findMatchingSubtag(Tag tag, String key, String value, BiPredicate<Tag, String> valueChecker) {
      return false;
   }

   public static TextColor findFirstColorCode(Component textComponent) {
      String rawTitle = textComponent.getString();

      for (int i = 0; i < rawTitle.length(); i += 2) {
         if (rawTitle.charAt(i) != 167) {
            return null;
         }

         try {
            ChatFormatting format = ChatFormatting.getByCode(rawTitle.charAt(i + 1));
            if (format != null && format.isColor()) {
               return TextColor.fromLegacyFormat(format);
            }
         } catch (StringIndexOutOfBoundsException var41) {
            return null;
         }
      }

      return null;
   }

   public static TextColor getColorForItem(ItemStack item, TextColor defaultColor) {
      TextColor result = null;
      result = item.getDisplayName().getStyle().getColor();
      if (item.getItem() != null
         && item.getItem().getName(item) != null
         && item.getItem().getName(item).getStyle() != null
         && item.getItem().getName(item).getStyle().getColor() != null) {
         result = item.getItem().getName(item).getStyle().getColor();
      }

      if (!item.getHoverName().getStyle().isEmpty() && item.getHoverName().getStyle().getColor() != null) {
         result = item.getHoverName().getStyle().getColor();
      }

      TextColor formattingColor = findFirstColorCode(item.getHoverName());
      if (formattingColor != null) {
         result = formattingColor;
      }

      Selectors.ColorCollector colorCollector = new Selectors.ColorCollector();
      item.getHoverName().getVisualOrderText().accept(colorCollector);
      if (colorCollector.getColor() != null) {
         result = colorCollector.getColor();
      }

      if (result == null || result.equals(item.getDisplayName().getStyle().getColor())) {
         Minecraft mc = Minecraft.getInstance();
         List<Component> lines = null;

         try {
            lines = item.getTooltipLines(TooltipContext.EMPTY, mc.player, Default.ADVANCED);
         } catch (Exception var8) {
         }

         if (lines != null && !lines.isEmpty()) {
            result = lines.get(0).getStyle().getColor();
         }
      }

      if (result == null) {
         result = defaultColor;
      }

      return result;
   }

   private static class ColorCollector implements FormattedCharSink {
      private TextColor color = null;

      private ColorCollector() {
      }

      public boolean accept(int index, Style style, int codePoint) {
         if (style.getColor() != null) {
            this.color = style.getColor();
            return false;
         } else {
            return true;
         }
      }

      public TextColor getColor() {
         return this.color;
      }
   }

   public record SelectorDocumentation(String name, String description, List<String> examples) {
      public SelectorDocumentation(String name, String description, String... examples) {
         this(name, description, Arrays.asList(examples));
      }
   }
}
