package com.lootbeams.compat.iris;

import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public class IrisCompat {
   public IrisCompat() {
   }

   public static boolean isIrisLoaded() {
      return FabricLoader.getInstance().isModLoaded("iris");
   }

   public static List<Integer> getVertexFormatsIndexes() {
      return List.of(10, 11, 12, 13, 14);
   }

   public static boolean isShaderPackInUse() {
      if (!isIrisLoaded()) {
         return false;
      } else {
         try {
            Object irisApiInstance = Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("getInstance").invoke(null);
            return (Boolean)Class.forName("net.irisshaders.iris.api.v0.IrisApi").getMethod("isShaderPackInUse").invoke(irisApiInstance);
         } catch (Exception var1) {
            var1.printStackTrace();
            return false;
         }
      }
   }
}
