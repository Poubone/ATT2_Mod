package com.lootbeams.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface PresetEvents {
   Event<PresetEvents.ApplyPreset> APPLY_PRESET = EventFactory.createArrayBacked(PresetEvents.ApplyPreset.class, listeners -> presetName -> {
      for (PresetEvents.ApplyPreset event : listeners) {
         event.onApplyPreset(presetName);
      }
   });

   public interface ApplyPreset {
      void onApplyPreset(String var1);
   }
}
