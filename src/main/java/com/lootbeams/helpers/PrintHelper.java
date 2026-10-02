package com.lootbeams.helpers;

import org.joml.Quaternionf;

public class PrintHelper {
   public PrintHelper() {
   }

   public static void printQuaternion(Quaternionf quaternion) {
      System.out.println("{ 'x': '" + quaternion.x + "', 'y': '" + quaternion.y + "', 'z': '" + quaternion.z + "', 'w': '" + quaternion.w + "'}");
   }
}
