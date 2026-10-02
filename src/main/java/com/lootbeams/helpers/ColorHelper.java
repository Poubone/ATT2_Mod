package com.lootbeams.helpers;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.network.chat.TextColor;

public class ColorHelper {
   public ColorHelper() {
   }

   public static int parseColor(String color) {
      if (color.startsWith("#")) {
         color = color.substring(1);
      }

      if (color.length() == 3 || color.length() == 4) {
         color = color.replaceAll(".", "$0$0");
      }

      if (color.length() == 6) {
         color = "FF" + color;
      }

      int i = Integer.parseUnsignedInt(color, 16);
      return Integer.compareUnsigned(i, 0) >= 0 && Integer.compareUnsigned(i, -1) <= 0 ? i : 0;
   }

   public static int darken(int color, float value) {
      int a = getAlpha(color);
      int r = (int)(getRed(color) * (1.0F - value));
      int g = (int)(getGreen(color) * (1.0F - value));
      int b = (int)(getBlue(color) * (1.0F - value));
      return build(a, r, g, b);
   }

   public static String darken(String color, float value) {
      int colorValue = parseColor(color);
      return toHexColor(darken(colorValue, value));
   }

   public static String toHexColor(int color) {
      return String.format("#%08X", color);
   }

   public static int getAlpha(int i) {
      return i <= 16777215 ? 255 : i >>> 24;
   }

   public static int getRed(int i) {
      return i >> 16 & 0xFF;
   }

   public static int getGreen(int i) {
      return i >> 8 & 0xFF;
   }

   public static int getBlue(int i) {
      return i & 0xFF;
   }

   public static ColorHelper.IntRGB getIntRGB(int color) {
      return new ColorHelper.IntRGB(getRed(color), getGreen(color), getBlue(color));
   }

   public static ColorHelper.FloatRGB getFloatRGB(int color) {
      return new ColorHelper.FloatRGB(getRed(color) / 255.0F, getGreen(color) / 255.0F, getBlue(color) / 255.0F);
   }

   public static int build(int A, int R, int G, int B) {
      return A << 24 | R << 16 | G << 8 | B;
   }

   public static class Color {
      public int R = 255;
      public int G = 255;
      public int B = 255;
      public int A = 255;
      public float fR = 1.0F;
      public float fG = 1.0F;
      public float fB = 1.0F;
      public float fA = 1.0F;
      public int H = 0;
      public int S = 0;
      public int V = 255;
      public float fH = 0.0F;
      public float fS = 0.0F;
      public float fV = 1.0F;

      public Color(int color) {
         this.setRgb(color);
         this.updateHsvValues();
         this.A = ColorHelper.getAlpha(color);
         this.fA = this.A / 255.0F;
      }

      public void setRgb(int color) {
         this.R = ColorHelper.getRed(color);
         this.G = ColorHelper.getGreen(color);
         this.B = ColorHelper.getBlue(color);
         this.fR = this.R / 255.0F;
         this.fG = this.G / 255.0F;
         this.fB = this.B / 255.0F;
      }

      public void updateRgbValues() {
         int rgb = java.awt.Color.HSBtoRGB(this.fH, this.fS, this.fV);
         this.setRgb(rgb & 16777215);
      }

      public void updateHsvValues() {
         float[] hsv = java.awt.Color.RGBtoHSB(this.R, this.G, this.B, null);
         this.fH = hsv[0];
         this.fS = hsv[1];
         this.fV = hsv[2];
         this.H = (int)(this.fH * 360.0F);
         this.S = (int)(this.fS * 255.0F);
         this.V = (int)(this.fV * 255.0F);
      }

      public int getRgb() {
         return ColorHelper.build(this.A, this.R, this.G, this.B);
      }

      public ColorHelper.Color applyModifiers(List<String> modifiers) {
         Map<Character, BiFunction<Integer, Integer, Integer>> modifierFuncs = Map.of('+', (v, a) -> v + a, '-', (v, a) -> v - a, '=', (v, a) -> a);

         for (String modifier : modifiers) {
            if (modifier.length() >= 3) {
               char type = modifier.toLowerCase().charAt(1);

               int amount;
               BiFunction<Integer, Integer, Integer> mod;
               try {
                  amount = Integer.parseInt(modifier.substring(2));
                  mod = modifierFuncs.get(modifier.charAt(0));
               } catch (Exception var9) {
                  continue;
               }

               if (mod != null) {
                  switch (type) {
                     case 'a':
                        this.A = Math.clamp((long)mod.apply(this.A, amount).intValue(), 0, 255);
                        this.fA = this.A / 255.0F;
                        break;
                     case 'b':
                        this.B = Math.clamp((long)mod.apply(this.B, amount).intValue(), 0, 255);
                        this.fB = this.B / 255.0F;
                        this.updateHsvValues();
                     case 'c':
                     case 'd':
                     case 'e':
                     case 'f':
                     case 'i':
                     case 'j':
                     case 'k':
                     case 'l':
                     case 'm':
                     case 'n':
                     case 'o':
                     case 'p':
                     case 'q':
                     case 't':
                     case 'u':
                     default:
                        break;
                     case 'g':
                        this.G = Math.clamp((long)mod.apply(this.G, amount).intValue(), 0, 255);
                        this.fG = this.G / 255.0F;
                        this.updateHsvValues();
                        break;
                     case 'h':
                        this.H = (mod.apply(this.H, amount) + 360) % 360;
                        this.fH = this.H / 360.0F;
                        this.updateRgbValues();
                        break;
                     case 'r':
                        this.R = Math.clamp((long)mod.apply(this.R, amount).intValue(), 0, 255);
                        this.fR = this.R / 255.0F;
                        this.updateHsvValues();
                        break;
                     case 's':
                        this.S = Math.clamp((long)mod.apply(this.S, amount).intValue(), 0, 255);
                        this.fS = this.S / 255.0F;
                        this.updateRgbValues();
                        break;
                     case 'v':
                        this.V = Math.clamp((long)mod.apply(this.V, amount).intValue(), 0, 255);
                        this.fV = this.V / 255.0F;
                        this.updateRgbValues();
                  }
               }
            }
         }

         return this;
      }

      public static ColorHelper.Color of(TextColor color) {
         return new ColorHelper.Color(color.getValue());
      }

      @Override
      public String toString() {
         return "Color[R:" + this.R + ",G:" + this.G + ",B:" + this.B + ",A:" + this.A + "]";
      }
   }

   public record FloatRGB(float R, float G, float B) {
      public ColorHelper.FloatRGB lighter(float value) {
         return new ColorHelper.FloatRGB(Math.min(this.R + value, 1.0F), Math.min(this.G + value, 1.0F), Math.min(this.B + value, 1.0F));
      }

      public ColorHelper.FloatRGB darken(float value) {
         return new ColorHelper.FloatRGB(Math.max(this.R - value, 0.0F), Math.max(this.G - value, 0.0F), Math.max(this.B - value, 0.0F));
      }

      public int pack() {
         return (int)(this.R * 255.0F) << 16 | (int)(this.G * 255.0F) << 8 | (int)(this.B * 255.0F);
      }

      public static ColorHelper.FloatRGB of(ColorHelper.Color color) {
         return new ColorHelper.FloatRGB(color.fR, color.fG, color.fB);
      }

      @Override
      public String toString() {
         return "RGB{R=" + this.R + ", G=" + this.G + ", B=" + this.B + "}";
      }
   }

   public record IntRGB(int R, int G, int B) {
      public ColorHelper.IntRGB lighter(int value) {
         return new ColorHelper.IntRGB(Math.min(this.R + value, 255), Math.min(this.G + value, 255), Math.min(this.B + value, 255));
      }

      public ColorHelper.IntRGB darken(int value) {
         return new ColorHelper.IntRGB(Math.max(this.R - value, 0), Math.max(this.G - value, 0), Math.max(this.B - value, 0));
      }

      public int pack() {
         return this.R << 16 | this.G << 8 | this.B;
      }

      public static ColorHelper.IntRGB of(ColorHelper.Color color) {
         return new ColorHelper.IntRGB(color.R, color.G, color.B);
      }

      @Override
      public String toString() {
         return "RGB{R=" + this.R + ", G=" + this.G + ", B=" + this.B + "}";
      }
   }
}
