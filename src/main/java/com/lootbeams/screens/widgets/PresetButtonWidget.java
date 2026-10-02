package com.lootbeams.screens.widgets;

import com.lootbeams.LootBeams;
import com.lootbeams.helpers.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class PresetButtonWidget extends Button {
   private static final WidgetSprites DEFAULT_TEXTURES = new WidgetSprites(
         LootBeams.id("textures/gui/preset_button/default/normal.png"),
         LootBeams.id("textures/gui/preset_button/default/disabled.png"),
         LootBeams.id("textures/gui/preset_button/default/highlighted.png"));
   private static final WidgetSprites ADD_TEXTURES = new WidgetSprites(
         LootBeams.id("textures/gui/preset_button/add/normal.png"),
         LootBeams.id("textures/gui/preset_button/add/disabled.png"),
         LootBeams.id("textures/gui/preset_button/add/highlighted.png"));
   private static final WidgetSprites CANCEL_TEXTURES = new WidgetSprites(
         LootBeams.id("textures/gui/preset_button/cancel/normal.png"),
         LootBeams.id("textures/gui/preset_button/cancel/disabled.png"),
         LootBeams.id("textures/gui/preset_button/cancel/highlighted.png"));

   private Type type;

   protected PresetButtonWidget(
         int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narrationSupplier, Type type) {
      super(x, y, width, height, message, onPress, narrationSupplier);
      this.type = type != null ? type : Type.Default;
   }

   private WidgetSprites getButtonTextures() {
      if (this.type == Type.Add) {
         return ADD_TEXTURES;
      }
      return this.type == Type.Cancel ? CANCEL_TEXTURES : DEFAULT_TEXTURES;
   }

   @Override
   protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta) {
      Identifier texture = this.getButtonTextures().get(this.active, this.isHoveredOrFocused());
      RenderHelper.blit(context, texture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, 0, this.getWidth(), this.getHeight(), 76, 19);
      int color = this.active ? 0xFFFFFF : 0xA0A0A0;
      context.drawString(
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + 8,
            this.getY() + (this.getHeight() - 8) / 2,
            color | Mth.ceil(this.alpha * 255.0F) << 24,
            false);
   }

   public static Builder customBuilder(Component message, OnPress onPress) {
      return new Builder(message, onPress);
   }

   public static class Builder {
      private final Component message;
      private final OnPress onPress;
      @Nullable
      private Tooltip tooltip;
      private Type type = Type.Default;
      private int x;
      private int y;
      private int width = 150;
      private int height = 20;
      private CreateNarration narrationSupplier = Button.DEFAULT_NARRATION;

      public Builder(Component message, OnPress onPress) {
         this.message = message;
         this.onPress = onPress;
      }

      public Builder position(int x, int y) {
         this.x = x;
         this.y = y;
         return this;
      }

      public Builder width(int width) {
         this.width = width;
         return this;
      }

      public Builder size(int width, int height) {
         this.width = width;
         this.height = height;
         return this;
      }

      public Builder dimensions(int x, int y, int width, int height) {
         return this.position(x, y).size(width, height);
      }

      public Builder tooltip(@Nullable Tooltip tooltip) {
         this.tooltip = tooltip;
         return this;
      }

      public Builder narrationSupplier(CreateNarration narrationSupplier) {
         this.narrationSupplier = narrationSupplier;
         return this;
      }

      public Builder type(Type type) {
         this.type = type;
         return this;
      }

      public PresetButtonWidget build() {
         PresetButtonWidget buttonWidget = new PresetButtonWidget(
               this.x, this.y, this.width, this.height, this.message, this.onPress, this.narrationSupplier, this.type);
         buttonWidget.setTooltip(this.tooltip);
         return buttonWidget;
      }
   }

   public enum Type {
      Default,
      Add,
      Cancel
   }
}
