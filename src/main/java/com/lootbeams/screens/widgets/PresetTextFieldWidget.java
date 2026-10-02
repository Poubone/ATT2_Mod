package com.lootbeams.screens.widgets;

import com.lootbeams.LootBeams;
import com.lootbeams.helpers.RenderHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PresetTextFieldWidget extends EditBox {
   private static final Identifier NORMAL = LootBeams.id("textures/gui/text_field/normal.png");
   private static final Identifier HIGHLIGHTED = LootBeams.id("textures/gui/text_field/highlighted.png");

   public PresetTextFieldWidget(Font font, int x, int y, int width, int height, Component message) {
      super(font, x, y, width, height, message);
      this.setBordered(false);
      this.setTextColor(0xE0E0E0);
      this.setMaxLength(64);
   }

   public String getText() {
      return this.getValue();
   }

   public void setText(String text) {
      this.setValue(text);
   }

   public void setChangedListener(java.util.function.Consumer<String> listener) {
      this.setResponder(listener);
   }

   public void setPlaceholder(Component placeholder) {
      this.setHint(placeholder);
   }

   @Override
   public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
      Identifier texture = this.isFocused() ? HIGHLIGHTED : NORMAL;
      RenderHelper.blit(context, texture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, 0, this.getWidth(), this.getHeight(), 78, 22);
      super.renderWidget(context, mouseX, mouseY, delta);
   }
}
