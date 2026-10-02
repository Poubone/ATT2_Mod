package com.lootbeams.helpers;

import com.lootbeams.LootBeams;
import com.lootbeams.features.CustomRarity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class RarityHelper {
   public RarityHelper() {
   }

   public static String getRarity(ItemStack stack) {
      String rarity = stack.getRarity().name().toLowerCase();
      rarity = rarity.replace(":", ".").replace("_", ".");
      CustomRarity customRarity = CustomRarity.fromItemStack(stack);
      if (customRarity != null) {
         return customRarity.getName();
      } else {
         String translatableKey = "lootbeams.rarity." + rarity;
         return I18n.exists(translatableKey) ? I18n.get(translatableKey, new Object[0]) : rarity;
      }
   }

   public static boolean rarityCheck(ItemStack itemStack, boolean isRare) {
      if (fr.poubone.att2.client.renderer.ItemRarity.fromStack(itemStack) != null) {
         return true;
      }
      CustomRarity customRarity = CustomRarity.fromItemStack(itemStack);
      return customRarity != null ? true : isRare || itemStack.getRarity() != Rarity.COMMON;
   }

   public static boolean alwaysHasRarity(ItemStack item) {
      List<String> overrides = LootBeams.config.alwaysDrawRaritiesOn;
      if (overrides.isEmpty()) {
         return false;
      } else {
         for (String name : overrides.stream().filter(s -> !s.isEmpty()).toList()) {
            Identifier registry = Identifier.tryParse(name.replace("#", ""));
            if (!name.contains(":") && BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals(name)) {
               return true;
            }

            if (registry != null) {
               if (name.startsWith("#")) {
                  Optional<Named<Item>> tag = BuiltInRegistries.ITEM.getTags().filter(pair -> pair.key().location().equals(registry)).findFirst();
                  if (tag.isPresent()
                     && tag.get().contains((Holder)BuiltInRegistries.ITEM.get((ResourceKey)BuiltInRegistries.ITEM.getResourceKey(item.getItem()).get()).get())) {
                     return true;
                  }
               }

               Optional<Item> registryItem = BuiltInRegistries.ITEM.getOptional(registry);
               if (registryItem.isPresent() && registryItem.get().asItem() == item.getItem()) {
                  return true;
               }
            }
         }

         return false;
      }
   }
}
