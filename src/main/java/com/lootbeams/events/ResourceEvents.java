package com.lootbeams.events;

import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;

public interface ResourceEvents {
   Event<ResourceEvents.ResourceReload> RESOURCE_RELOAD = EventFactory.createArrayBacked(
      ResourceEvents.ResourceReload.class, listeners -> (synchronizer, resourceManager, prepareExecutor, applyExecutor) -> {
         for (ResourceEvents.ResourceReload event : listeners) {
            event.onResourceReload(synchronizer, resourceManager, prepareExecutor, applyExecutor);
         }
      }
   );

   public interface ResourceReload {
      void onResourceReload(PreparationBarrier var1, ResourceManager var2, Executor var3, Executor var4);
   }
}
