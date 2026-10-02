package com.lootbeams.screens;

import com.lootbeams.LootBeams;
import com.lootbeams.helpers.RenderHelper;
import com.lootbeams.managers.PresetManager;
import com.lootbeams.screens.widgets.PresetButtonWidget;
import com.lootbeams.screens.widgets.PresetListWidget;
import com.lootbeams.screens.widgets.PresetTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class LootBeamsPresetManagerScreen extends Screen {
   private static final Identifier MODAL_TEXTURE = LootBeams.id("textures/gui/screen/modal.png");
   private static final int BACKGROUND_WIDTH = 184;
   private static final int BACKGROUND_HEIGHT = 152;
   private final Screen parent;
   private PresetTextFieldWidget presetNameField;
   private PresetButtonWidget savePresetButton;
   private PresetButtonWidget loadPresetButton;
   private PresetListWidget presetListWidget;
   private PresetListWidget.PresetEntry addEntry;

   public LootBeamsPresetManagerScreen(Screen parent) {
      super(Component.translatable("lootbeams.screen.presets.title"));
      this.parent = parent;
   }

   private void reinitPresetListWidget() {
      PresetManager.getPresetFiles();
      this.presetListWidget.clearEntries();
      this.presetListWidget.addEntries(PresetManager.getPresetNames());
      this.addEntry = this.presetListWidget.addEntry("+", PresetButtonWidget.Type.Add);
   }

   @Override
   protected void init() {
      int topPos = this.height / 2 - 76;
      int bottomPos = this.height / 2 + 76;
      int leftPos = this.width / 2 - 92;
      int rightPos = this.width / 2 + 92;
      int topHeaderPos = topPos + 29;
      this.presetListWidget = new PresetListWidget(this.minecraft, 90, 118, topHeaderPos + 2, 20);
      this.presetListWidget.setPosition(leftPos + 5, topHeaderPos + 2);
      this.reinitPresetListWidget();
      this.addRenderableWidget(this.presetListWidget);
      this.presetNameField = new PresetTextFieldWidget(
            this.font, rightPos - 84, topHeaderPos + 10, 78, 22, Component.translatable("lootbeams.screen.presets.presetNameTextField"));
      this.presetNameField.setPlaceholder(Component.translatable("lootbeams.screen.presets.presetNameTextField"));
      this.addWidget(this.presetNameField);
      this.presetNameField.setChangedListener(value -> {
         if (this.savePresetButton != null) {
            this.savePresetButton.active = !value.trim().isEmpty();
         }
      });
      this.savePresetButton = PresetButtonWidget.customBuilder(
            Component.translatable("lootbeams.screen.presets.button.save"),
            btn -> {
               String presetName = this.presetNameField.getText().trim();
               if (presetName.isEmpty()) {
                  this.minecraft.getToastManager().addToast(SystemToast.multiline(
                        this.minecraft,
                        SystemToastId.WORLD_ACCESS_FAILURE,
                        Component.literal("LootBeams"),
                        Component.translatable("lootbeams.notification.savePreset.notEnteredPresetName")));
               } else {
                  PresetManager.savePreset(PresetManager.getPresetFileName(presetName));
                  this.reinitPresetListWidget();
                  btn.active = false;
               }
            })
            .position(rightPos - 83, topHeaderPos + 47)
            .size(76, 19)
            .build();
      this.savePresetButton.active = false;
      this.loadPresetButton = PresetButtonWidget.customBuilder(Component.translatable("lootbeams.screen.presets.button.load"), btn -> {
         PresetListWidget.PresetEntry presetEntry = this.presetListWidget.getSelectedEntry();
         if (presetEntry != null) {
            PresetManager.loadAndApplyPreset(presetEntry.getPresetName());
         }
      }).position(rightPos - 83, topHeaderPos + 47).size(76, 19).build();
      this.loadPresetButton.active = false;
      PresetButtonWidget cancelButton = PresetButtonWidget.customBuilder(
            Component.translatable("lootbeams.screen.presets.button.cancel"),
            btn -> Minecraft.getInstance().setScreen(this.parent))
            .position(rightPos - 83, bottomPos - 27)
            .size(76, 19)
            .type(PresetButtonWidget.Type.Cancel)
            .build();
      this.addWidget(this.savePresetButton);
      this.addWidget(this.loadPresetButton);
      this.addRenderableWidget(cancelButton);
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      return event.key() != 258 && event.key() != 264 && event.key() != 265 ? super.keyPressed(event) : false;
   }

   @Override
   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      if (this.presetListWidget.getSelectedEntry() == this.addEntry) {
         this.presetNameField.render(context, mouseX, mouseY, delta);
         this.savePresetButton.render(context, mouseX, mouseY, delta);
      } else {
         PresetListWidget.PresetEntry presetEntry = this.presetListWidget.getSelectedEntry();
         this.loadPresetButton.active = presetEntry != null && !presetEntry.getPresetName().equals(LootBeams.config.selectedPreset);
         this.loadPresetButton.render(context, mouseX, mouseY, delta);
      }
   }

   @Override
   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.renderBackground(context, mouseX, mouseY, delta);
      int i = (this.width - BACKGROUND_WIDTH) / 2;
      int j = (this.height - BACKGROUND_HEIGHT) / 2;
      RenderHelper.blit(context, MODAL_TEXTURE, i, j, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 256, 256);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }
}
