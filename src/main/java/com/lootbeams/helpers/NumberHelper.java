package com.lootbeams.helpers;

import net.minecraft.util.Mth;

public class NumberHelper {
   public NumberHelper() {
   }

   public static <T extends Number> double mapRange(T n, T fromStart, T fromEnd, T toStart, T toEnd) {
      return (n.doubleValue() - fromStart.doubleValue()) / (fromEnd.doubleValue() - fromStart.doubleValue()) * (toEnd.doubleValue() - toStart.doubleValue())
         + toStart.doubleValue();
   }

   public static <T extends Number> double clampedMapRange(T n, T fromStart, T fromEnd, T toStart, T toEnd) {
      double value = mapRange(n, fromStart, fromEnd, toStart, toEnd);
      double min = Math.min(toStart.doubleValue(), toEnd.doubleValue());
      double max = Math.max(toStart.doubleValue(), toEnd.doubleValue());
      return Mth.clamp(value, min, max);
   }

   public static float smoothValue(float value, float currentTime, float duration) {
      return Mth.lerp(Math.min(currentTime, duration) / duration, 0.0F, value);
   }
}
