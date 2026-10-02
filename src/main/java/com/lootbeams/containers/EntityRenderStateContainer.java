package com.lootbeams.containers;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public class EntityRenderStateContainer {
   private static Map<Entity, EntityRenderState> ENTITY_RENDER_STATES = new HashMap<>();

   public EntityRenderStateContainer() {
   }

   public static EntityRenderState getRenderState(Entity entity) {
      return ENTITY_RENDER_STATES.get(entity);
   }

   public static void setRenderState(Entity entity, EntityRenderState renderState) {
      ENTITY_RENDER_STATES.put(entity, renderState);
   }

   public static boolean hasRenderState(Entity entity) {
      return ENTITY_RENDER_STATES.containsKey(entity);
   }
}
