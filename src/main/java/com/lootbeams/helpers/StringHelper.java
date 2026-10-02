package com.lootbeams.helpers;

public class StringHelper {
   public StringHelper() {
   }

   public static String capitalize(String str) {
      return str != null && !str.isEmpty() ? str.substring(0, 1).toUpperCase() + str.substring(1) : "";
   }

   public static String camelToSnake(String str) {
      return str.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2").toLowerCase();
   }

   public static String toBinaryName(String mapName) {
      return "L" + mapName.replace('.', '/') + ";";
   }
}
