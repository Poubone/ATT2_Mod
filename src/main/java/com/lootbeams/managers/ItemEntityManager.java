package com.lootbeams.managers;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class ItemEntityManager {
   private static final Map<ItemStack, ItemEntity> stackToEntityMap = new WeakHashMap<>();
   private static final Map<ItemEntity, ItemStack> entityToStackMap = new WeakHashMap<>();
   private static final Map<EntityRenderState, ItemStack> entityRenderStateToStackMap = new WeakHashMap<>();

   public ItemEntityManager() {
   }

   public static void track(ItemStack itemStack, ItemEntity entity) {
      stackToEntityMap.put(itemStack, entity);
      entityToStackMap.put(entity, itemStack);
   }

   public static void track(EntityRenderState renderState, ItemStack itemStack) {
      entityRenderStateToStackMap.put(renderState, itemStack);
   }

   public static ItemEntity getEntityForStack(ItemStack stack) {
      return stackToEntityMap.get(stack);
   }

   public static ItemStack getItemStack(ItemEntity entity) {
      return entityToStackMap.get(entity);
   }

   public static ItemStack getItemStack(EntityRenderState renderState) {
      return entityRenderStateToStackMap.get(renderState);
   }

   public static void untrack(ItemStack itemStack) {
      ItemEntity entity = stackToEntityMap.remove(itemStack);
      entityToStackMap.remove(entity);
   }

   public static void untrack(EntityRenderState renderState) {
      entityRenderStateToStackMap.remove(renderState);
   }
}
