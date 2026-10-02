package com.lootbeams.events;

import com.lootbeams.contexts.WorldRendererContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public interface RenderEvents {
   Event<RenderEvents.HudRender> HUD = EventFactory.createArrayBacked(RenderEvents.HudRender.class, listeners -> (drawContext, tickCounter) -> {
      for (RenderEvents.HudRender listener : listeners) {
         listener.onHud(drawContext, tickCounter);
      }
   });
   Event<RenderEvents.BeforeParticles> BEFORE_PARTICLES = EventFactory.createArrayBacked(
      RenderEvents.BeforeParticles.class, listeners -> worldRendererContext -> {
         for (RenderEvents.BeforeParticles listener : listeners) {
            listener.beforeParticles(worldRendererContext);
         }
      }
   );
   Event<RenderEvents.AfterTranslucent> AFTER_TRANSLUCENT = EventFactory.createArrayBacked(
      RenderEvents.AfterTranslucent.class, listeners -> worldRendererContext -> {
         for (RenderEvents.AfterTranslucent listener : listeners) {
            listener.afterTranslucent(worldRendererContext);
         }
      }
   );
   Event<RenderEvents.AfterWeather> AFTER_WEATHER = EventFactory.createArrayBacked(RenderEvents.AfterWeather.class, listeners -> worldRendererContext -> {
      for (RenderEvents.AfterWeather listener : listeners) {
         listener.afterWeather(worldRendererContext);
      }
   });
   Event<RenderEvents.BeforeEnd> BEFORE_END = EventFactory.createArrayBacked(RenderEvents.BeforeEnd.class, listeners -> worldRendererContext -> {
      for (RenderEvents.BeforeEnd listener : listeners) {
         listener.beforeEnd(worldRendererContext);
      }
   });
   Event<RenderEvents.End> END = EventFactory.createArrayBacked(RenderEvents.End.class, listeners -> worldRendererContext -> {
      for (RenderEvents.End listener : listeners) {
         listener.onEnd(worldRendererContext);
      }
   });

   public interface AfterTranslucent {
      void afterTranslucent(WorldRendererContext var1);
   }

   public interface AfterWeather {
      void afterWeather(WorldRendererContext var1);
   }

   public interface BeforeEnd {
      void beforeEnd(WorldRendererContext var1);
   }

   public interface BeforeParticles {
      void beforeParticles(WorldRendererContext var1);
   }

   public interface End {
      void onEnd(WorldRendererContext var1);
   }

   public interface HudRender {
      void onHud(GuiGraphics var1, DeltaTracker var2);
   }
}
