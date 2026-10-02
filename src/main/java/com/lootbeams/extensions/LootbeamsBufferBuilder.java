package com.lootbeams.extensions;

public interface LootbeamsBufferBuilder {
   LootbeamsBufferBuilder color1(int var1, int var2, int var3, int var4);

   LootbeamsBufferBuilder uvCenter(float var1, float var2);

   LootbeamsBufferBuilder uvSize(float var1, float var2);

   LootbeamsBufferBuilder shortCustomData(float var1, float var2);

   LootbeamsBufferBuilder longCustomData(float var1, float var2, float var3, float var4);
}
