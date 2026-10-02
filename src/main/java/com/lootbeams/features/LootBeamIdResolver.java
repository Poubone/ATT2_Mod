package com.lootbeams.features;

import java.util.Map;

public final class LootBeamIdResolver {
   private LootBeamIdResolver() {
   }

   public static String firstNonBlank(String... ids) {
      if (ids == null) {
         return "";
      }
      for (String id : ids) {
         if (id != null && !id.isEmpty()) {
            return id;
         }
      }
      return "";
   }

   public static String firstCached(Map<String, ?> cache, String... ids) {
      if (ids == null || cache == null) {
         return "";
      }
      for (String id : ids) {
         if (id != null && !id.isEmpty() && cache.containsKey(id)) {
            return id;
         }
      }
      return "";
   }
}
