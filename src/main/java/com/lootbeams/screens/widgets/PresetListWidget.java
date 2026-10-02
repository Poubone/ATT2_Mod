package com.lootbeams.screens.widgets;

import com.lootbeams.LootBeams;
import com.lootbeams.helpers.RenderHelper;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class PresetListWidget extends ObjectSelectionList<PresetListWidget.PresetEntry> {
   private static final Identifier SCROLLER_TEXTURE = LootBeams.id("textures/gui/scroller/scroller.png");
   private static final Identifier SCROLLER_BACKGROUND_TEXTURE = LootBeams.id("textures/gui/scroller/scroller_background.png");
   private PresetEntry selectedEntry;
   private boolean isScrolling;
   private double dragOffsetY;

   public PresetListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
      super(client, width, height, y, itemHeight);
   }

   public void addEntries(Iterable<String> presetNames) {
      for (String presetName : presetNames) {
         PresetEntry entry = new PresetEntry(presetName, this, PresetButtonWidget.Type.Default);
         this.addEntry(entry);
         if (presetName.equals(LootBeams.config.selectedPreset)) {
            this.selectedEntry = entry;
         }
      }
   }

   @Override
   public void clearEntries() {
      super.clearEntries();
   }

   public PresetEntry addEntry(String name, PresetButtonWidget.Type buttonType) {
      PresetEntry entry = new PresetEntry(name, this, buttonType);
      this.addEntry(entry);
      return entry;
   }

   private int getScrollbarWidth() {
      return 6;
   }

   @Override
   protected int scrollBarX() {
      return this.getX() + this.getWidth() - this.getScrollbarWidth() - 6 + 4;
   }

   @Override
   public int getRowWidth() {
      return this.getWidth() - this.getScrollbarWidth() - 6 - 3;
   }

   @Override
   public int getRowLeft() {
      return this.getX() + 3;
   }

   @Override
   public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
      this.enableScissor(context);
      this.renderListItems(context, mouseX, mouseY, delta);
      context.disableScissor();
      if (this.scrollbarVisible()) {
         int i = this.scrollBarX();
         int j = (int) ((float) (this.height * this.height) / this.contentHeight());
         j = Mth.clamp(j, 32, this.height - 8);
         int k = (int) this.scrollAmount() * (this.height - j) / Math.max(1, this.maxScrollAmount()) + this.getY();
         if (k < this.getY()) {
            k = this.getY();
         }
         RenderHelper.blit(context, SCROLLER_BACKGROUND_TEXTURE, i, this.getY() - 1, 6, this.getHeight() + 2, 0, 0);
         this.renderScroller(context, i, k - 1, 6, j + 2);
      }
   }

   private void renderScroller(GuiGraphics context, int x, int y, int width, int height) {
      int topHeight = 6;
      int bottomHeight = 6;
      int middleHeight = 18;
      RenderHelper.blit(context, SCROLLER_TEXTURE, x, y, width, topHeight, 0, 0, width, topHeight);
      int remainingHeight = height - topHeight - bottomHeight;
      int middleRepeats = remainingHeight / middleHeight;
      int middleRemainder = remainingHeight % middleHeight;
      for (int i = 0; i < middleRepeats; i++) {
         RenderHelper.blit(context, SCROLLER_TEXTURE, x, y + topHeight + i * middleHeight, width, middleHeight, 0, 7, width, middleHeight);
      }
      if (middleRemainder > 0) {
         RenderHelper.blit(context, SCROLLER_TEXTURE, x, y + topHeight + middleRepeats * middleHeight, width, middleRemainder, 0, 7, width, middleRemainder);
      }
      RenderHelper.blit(context, SCROLLER_TEXTURE, x, y + height - bottomHeight, width, bottomHeight, 0, 26, width, bottomHeight);
   }

   public PresetEntry getSelectedEntry() {
      return this.selectedEntry;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
      if (!this.isScrolling) {
         return false;
      }
      if (event.y() >= this.getY() && event.y() <= this.getBottom()) {
         double scrollRange = this.maxScrollAmount();
         double scrollbarRange = this.getHeight();
         double scrollPercentage = (event.y() - this.getY() - this.dragOffsetY) / (scrollbarRange - this.getScrollbarHeight());
         this.setScrollAmount(scrollRange * scrollPercentage);
         return true;
      }
      return super.mouseDragged(event, deltaX, deltaY);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (event.button() == 0) {
         int scrollbarY = this.getScrollbarY();
         int scrollbarHeight = this.getScrollbarHeight();
         if (event.x() >= this.scrollBarX()
               && event.x() <= this.scrollBarX() + this.getScrollbarWidth()
               && event.y() >= scrollbarY
               && event.y() <= scrollbarY + scrollbarHeight) {
            this.dragOffsetY = event.y() - scrollbarY;
            this.isScrolling = true;
            return true;
         }
      }
      return super.mouseClicked(event, doubled);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0) {
         this.isScrolling = false;
      }
      return super.mouseReleased(event);
   }

   private int getScrollbarY() {
      int scrollbarHeight = this.getScrollbarHeight();
      int maxScrollY = this.maxScrollAmount();
      if (maxScrollY <= 0) {
         return this.getY();
      }
      int scrollbarPositionY = (int) this.scrollAmount() * (this.height - scrollbarHeight) / maxScrollY + this.getY();
      return Math.max(scrollbarPositionY, this.getY());
   }

   private int getScrollbarHeight() {
      int j = (int) ((float) (this.height * this.height) / Math.max(1, this.contentHeight()));
      return Mth.clamp(j, 32, this.height - 8);
   }

   public static class PresetEntry extends ObjectSelectionList.Entry<PresetEntry> {
      private final String presetName;
      private final PresetButtonWidget button;
      private final PresetListWidget parentWidget;

      public PresetEntry(String presetName, PresetListWidget parentWidget, PresetButtonWidget.Type buttonType) {
         this.presetName = presetName;
         this.parentWidget = parentWidget;
         this.button = PresetButtonWidget.customBuilder(Component.literal(presetName), btn -> {}).type(buttonType).build();
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
         if (event.button() == 0) {
            if (this.parentWidget.selectedEntry != this) {
               this.parentWidget.selectedEntry = this;
            } else {
               this.parentWidget.selectedEntry = null;
            }
            return true;
         }
         return false;
      }

      public String getPresetName() {
         return this.presetName;
      }

      @Override
      public Component getNarration() {
         return Component.literal(this.presetName);
      }

      @Override
      public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         boolean currentEntrySelected = this.parentWidget.getSelectedEntry() == this;
         this.button.setRectangle(this.getContentWidth(), this.getContentHeight(), this.getContentX(), this.getContentY());
         this.button.active = currentEntrySelected || Objects.equals(this.presetName, "+");
         this.button.render(context, mouseX, mouseY, tickDelta);
      }
   }
}
