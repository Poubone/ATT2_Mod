package com.lootbeams.helpers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ItemHelper {
   public static boolean isItemInRegistryList(List<String> registryNames, Item item) {
      if (!registryNames.isEmpty()) {
         for (String id : registryNames.stream().filter(s -> !s.isEmpty()).toList()) {
            if (!id.contains(":") && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(id)) {
               return true;
            }
            Identifier itemResource = Identifier.tryParse(id);
            if (itemResource != null && BuiltInRegistries.ITEM.getValue(itemResource) == item) {
               return true;
            }
         }
      }
      return false;
   }

   public static boolean isEquipmentItem(ItemStack itemStack) {
      return itemStack.has(DataComponents.EQUIPPABLE)
            || itemStack.has(DataComponents.WEAPON)
            || itemStack.has(DataComponents.TOOL)
            || itemStack.has(DataComponents.BLOCKS_ATTACKS)
            || itemStack.has(DataComponents.PIERCING_WEAPON)
            || itemStack.has(DataComponents.KINETIC_WEAPON);
   }

   public static boolean isBundle(ItemStack itemStack) {
      return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString().contains("bundle");
   }
}
