package com.lootbeams.dconfig.events;

public interface ConfigEvents {
   EventFactory.Event<ConfigEvents.ConfigSave> SAVE = EventFactory.createArrayBacked(ConfigEvents.ConfigSave.class, listeners -> () -> {
      for (ConfigEvents.ConfigSave event : listeners) {
         event.onConfigSave();
      }
   });
   EventFactory.Event<ConfigEvents.ConfigChange> CHANGE = EventFactory.createArrayBacked(ConfigEvents.ConfigChange.class, listeners -> () -> {
      for (ConfigEvents.ConfigChange event : listeners) {
         event.onConfigChange();
      }
   });

   public interface ConfigChange {
      void onConfigChange();
   }

   public interface ConfigSave {
      void onConfigSave();
   }
}
