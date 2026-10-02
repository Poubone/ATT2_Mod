package com.lootbeams.render;

import com.lootbeams.compat.iris.IrisCompat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import java.util.ArrayList;
import java.util.List;

public class CustomVertexFormats {
   public static VertexFormatElement COLOR1;
   public static VertexFormatElement UV_CENTER;
   public static VertexFormatElement UV_SIZE;
   public static VertexFormatElement CUSTOM_DATA;
   public static VertexFormat POSITION_TEX_COLOR0_COLOR1_CUSTOM;
   public static VertexFormat POSITION_TEX_COLOR0_COLOR1_CENTER;
   private static final List<Integer> EMPTY_INDEXES = new ArrayList<>();

   private static int getAvailableIndex() {
      return EMPTY_INDEXES.removeFirst();
   }

   static {
      for (int i = 0; i < 32; i++) {
         VertexFormatElement element = VertexFormatElement.byId(i);
         if ((!IrisCompat.isIrisLoaded() || !IrisCompat.getVertexFormatsIndexes().contains(i)) && element == null) {
            EMPTY_INDEXES.add(i);
         }
      }

      // 1.21.11: a second Usage.COLOR is rejected ("Multiple vertex elements of the
      // same type other than UVs are not supported" when index != 0). COLOR/NORMAL
      // also go through the normalized VAO path. Store Color1 as GENERIC+FLOAT so
      // VertexArrayCache uses the same non-normalized attrib format as POSITION.
      COLOR1 = VertexFormatElement.register(getAvailableIndex(), 0, Type.FLOAT, Usage.GENERIC, 4);
      UV_CENTER = VertexFormatElement.register(getAvailableIndex(), 0, Type.FLOAT, Usage.UV, 2);
      UV_SIZE = VertexFormatElement.register(getAvailableIndex(), 0, Type.FLOAT, Usage.UV, 2);
      CUSTOM_DATA = VertexFormatElement.register(getAvailableIndex(), 0, Type.FLOAT, Usage.GENERIC, 4);
      POSITION_TEX_COLOR0_COLOR1_CUSTOM = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("Color1", COLOR1)
            .add("CustomData", CUSTOM_DATA)
            .build();
      POSITION_TEX_COLOR0_COLOR1_CENTER = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("Color1", COLOR1)
            .add("CenterUV", UV_CENTER)
            .add("SizeUV", UV_SIZE)
            .add("GradientBounds", CUSTOM_DATA)
            .build();
   }
}
