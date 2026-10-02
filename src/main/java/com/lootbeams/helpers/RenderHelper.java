package com.lootbeams.helpers;

import com.lootbeams.shaders.LootBeamShaders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public class RenderHelper {
   public static void blit(
         GuiGraphics context, Identifier texture, int x, int y, int width, int height, int texX, int texY, int regionWidth, int regionHeight) {
      blit(context, texture, x, y, width, height, texX, texY, regionWidth, regionHeight, 256, 256);
   }

   public static void blit(GuiGraphics context, Identifier texture, int x, int y, int width, int height, int texX, int texY) {
      blit(context, texture, x, y, width, height, texX, texY, width, height);
   }

   public static void blit(GuiGraphics context, Identifier texture, int x, int y) {
      blit(context, texture, x, y, 16, 16, 0, 0);
   }

   public static void blit(
         GuiGraphics context,
         Identifier texture,
         int x,
         int y,
         int width,
         int height,
         float texX,
         float texY,
         int regionWidth,
         int regionHeight,
         int textureWidth,
         int textureHeight) {
      context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, texX, texY, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
   }

   public static void drawColored(
         GuiGraphics context,
         Identifier texture,
         int x,
         int y,
         int width,
         int height,
         float u,
         float v,
         int regionWidth,
         int regionHeight,
         int textureWidth,
         int textureHeight,
         String color) {
      blit(context, texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
   }

   public static void drawColored(
         GuiGraphics context, Identifier texture, int x, int y, int width, int height, int texX, int texY, int regionWidth, int regionHeight, String color) {
      blit(context, texture, x, y, width, height, texX, texY, regionWidth, regionHeight);
   }

   public static void drawTextureAt(GuiGraphics context, Identifier texture, int x, int y) {
      blit(context, texture, x, y);
   }

   public static net.minecraft.client.renderer.Rect2i getSpritePositionAndSize(TextureAtlasSprite sprite) {
      return new net.minecraft.client.renderer.Rect2i(sprite.getX(), sprite.getY(), sprite.contents().width(), sprite.contents().height());
   }

   public static float overlayAverage() {
      return LootBeamShaders.getAverageColor(LootBeamShaders.Shader.PARTICLE_OVERLAY);
   }
}
