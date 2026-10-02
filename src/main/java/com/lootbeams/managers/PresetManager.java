package com.lootbeams.managers;

import com.lootbeams.LootBeams;
import com.lootbeams.events.PresetEvents;
import com.lootbeams.helpers.FileHelper;
import com.lootbeams.helpers.StringHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class PresetManager {
   private static final String PRESET_ASSETS_DIRECTORY = "data/presets";
   private static final String PRESET_CONFIG_DIRECTORY = "lootbeams/presets";
   private static final String PRESET_EXTENSION = ".json";
   private static Map<Identifier, Resource> RESOURCE_PRESETS_MAP = new HashMap<>();
   private static List<PresetManager.Preset> presetList = null;

   public PresetManager() {
   }

   public static void init() {
      getPresetFiles();
   }

   public static String getPresetFormattedName(String path) {
      String result = path;
      if (path.contains(".")) {
         result = path.substring(path.lastIndexOf(47) + 1, path.lastIndexOf(46));
      }

      if (result.contains("_")) {
         result = result.replace('_', ' ');
      }

      return StringHelper.capitalize(result);
   }

   public static String getPresetFileName(String presetName) {
      return presetName.replace(' ', '_');
   }

   public static PresetManager.Preset getFirstPreset() {
      return presetList != null && !presetList.isEmpty() ? presetList.getFirst() : null;
   }

   public static String getFirstPresetName() {
      PresetManager.Preset preset = getFirstPreset();
      return preset == null ? "" : preset.getDisplayName();
   }

   public static Set<String> getPresetNames() {
      getPresetFiles();
      Set<String> presetNames = new HashSet<>();
      if (presetList != null && !presetList.isEmpty()) {
         for (PresetManager.Preset preset : presetList) {
            presetNames.add(preset.getDisplayName());
         }
      }
      return presetNames;
   }

   public static Map<String, Identifier> getDefaultPresets() {
      Map<String, Identifier> defaultPresetMap = new HashMap<>();
      if (presetList != null && !presetList.isEmpty()) {
         for (PresetManager.Preset preset : presetList) {
            if (preset.getType() == PresetManager.PresetType.IDENTIFIER) {
               defaultPresetMap.put(preset.getDisplayName(), preset.getIdentifier());
            }
         }

         return defaultPresetMap;
      } else {
         return defaultPresetMap;
      }
   }

   public static Map<String, File> getCustomPresets() {
      Map<String, File> customPresetMap = new HashMap<>();
      if (presetList != null && !presetList.isEmpty()) {
         for (PresetManager.Preset preset : presetList) {
            if (preset.getType() == PresetManager.PresetType.FILE) {
               customPresetMap.put(preset.getDisplayName(), preset.getPresetFile());
            }
         }

         return customPresetMap;
      } else {
         return customPresetMap;
      }
   }

   public static void onResourcesReload(ResourceManager resourceManager) {
      RESOURCE_PRESETS_MAP = resourceManager.listResources("data/presets", identifier -> identifier.getPath().endsWith(".json"));
      presetList = null;
      getPresetFiles();
   }

   public static List<PresetManager.Preset> getPresetFiles() {
      List<PresetManager.Preset> presets = new ArrayList<>();

      for (Identifier presetId : RESOURCE_PRESETS_MAP.keySet()) {
         presets.add(new PresetManager.Preset(getPresetFormattedName(presetId.getPath()), presetId));
      }

      try {
         Path presetDirPath = FabricLoader.getInstance().getConfigDir().resolve("lootbeams/presets");
         if (Files.exists(presetDirPath)) {
            Files.list(presetDirPath)
               .filter(x$0 -> Files.isRegularFile(x$0))
               .filter(path -> path.toString().endsWith(".json"))
               .forEach(path -> presets.add(new PresetManager.Preset(getPresetFormattedName(path.getFileName().toString()), path.toFile())));
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      presetList = presets;
      return presets;
   }

   public static void loadPreset(File presetFile) {
      LootBeams.configManager.load(presetFile);
   }

   public static void loadPreset(Identifier id) {
      Resource presetResource = RESOURCE_PRESETS_MAP.get(id);
      if (presetResource != null) {
         try {
            LootBeams.configManager.load(presetResource.open());
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }
   }

   public static void savePreset(String presetName) {
      Path newPresetPath = FabricLoader.getInstance().getConfigDir().resolve("lootbeams/presets/" + presetName + ".json");
      File newPresetFile = FileHelper.createPathIfNotExists(newPresetPath.toString());
      LootBeams.configManager.save(newPresetFile);
      Minecraft client = Minecraft.getInstance();
      client.getToastManager()
         .addToast(
            SystemToast.multiline(
               client,
               SystemToastId.NARRATOR_TOGGLE,
               Component.nullToEmpty("LootBeams"),
               Component.translatable("lootbeams.notification.savePreset.text", new Object[]{getPresetFormattedName(presetName)})
            )
         );
   }

   public static boolean tryApplyPreset(String presetName) {
      if (LootBeams.config.selectedPreset.equals(presetName)) {
         return false;
      } else {
         Map<String, Identifier> defaultPresets = getDefaultPresets();
         Map<String, File> customPresets = getCustomPresets();
         if (defaultPresets.containsKey(presetName)) {
            Identifier presetId = defaultPresets.get(presetName);
            loadPreset(presetId);
            LootBeams.config.selectedPreset = presetName;
            return true;
         } else if (customPresets.containsKey(presetName)) {
            File presetFile = customPresets.get(presetName);
            loadPreset(presetFile);
            LootBeams.config.selectedPreset = presetName;
            return true;
         } else {
            return false;
         }
      }
   }

   public static void loadAndApplyPreset(String presetName) {
      if (tryApplyPreset(presetName)) {
         Minecraft client = Minecraft.getInstance();
         client.getToastManager()
            .addToast(
               SystemToast.multiline(
                  client,
                  SystemToastId.NARRATOR_TOGGLE,
                  Component.nullToEmpty("LootBeams"),
                  Component.translatable("lootbeams.notification.loadPreset.text", new Object[]{presetName})
               )
            );
         LootBeams.configManager.save();
         ((PresetEvents.ApplyPreset)PresetEvents.APPLY_PRESET.invoker()).onApplyPreset(presetName);
      }
   }

   public static class Preset {
      private final PresetManager.PresetType type;
      private final String displayName;
      private final Identifier identifier;
      private final File presetFile;

      public Preset(String displayName, Identifier identifier) {
         this.type = PresetManager.PresetType.IDENTIFIER;
         this.displayName = displayName;
         this.identifier = identifier;
         this.presetFile = null;
      }

      public Preset(String displayName, File presetFile) {
         this.type = PresetManager.PresetType.FILE;
         this.displayName = displayName;
         this.identifier = null;
         this.presetFile = presetFile;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public Identifier getIdentifier() {
         return this.identifier;
      }

      public File getPresetFile() {
         return this.presetFile;
      }

      public PresetManager.PresetType getType() {
         return this.type;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }

   public static enum PresetType {
      IDENTIFIER,
      FILE;

      private PresetType() {
      }
   }
}
