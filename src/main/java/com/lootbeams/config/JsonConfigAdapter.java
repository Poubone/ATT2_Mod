package com.lootbeams.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.lootbeams.dconfig.DynamicConfig;
import com.lootbeams.dconfig.interfaces.DynamicConfigAdapter;
import com.lootbeams.features.BeamOpacityOnApproach;
import com.lootbeams.features.BeamSizeOnApproach;
import com.lootbeams.helpers.FileHelper;
import com.lootbeams.managers.CrashManager;
import com.lootbeams.managers.GlowEffectManager;
import com.lootbeams.managers.ParticleManager;
import com.lootbeams.shaders.LootBeamShaders;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;

public class JsonConfigAdapter implements DynamicConfigAdapter<Configuration> {
   private final Map<Class<?>, DynamicConfig.ConfigManager.LoadConsumer<Object, DynamicConfig.Control.Field, JsonObject>> TYPE_LOADERS = new HashMap<>();
   private final Map<Class<?>, DynamicConfig.ConfigManager.SaveConsumer<JsonElement, Object>> TYPE_SAVERS = new HashMap<>();

   public JsonConfigAdapter() {
   }

   public void registerTypeLoadConsumer(Class<?> type, DynamicConfig.ConfigManager.LoadConsumer<Object, DynamicConfig.Control.Field, JsonObject> consumer) {
      if (!this.TYPE_LOADERS.containsKey(type)) {
         this.TYPE_LOADERS.put(type, consumer);
      }
   }

   public void registerTypeSaveConsumer(Class<?> type, DynamicConfig.ConfigManager.SaveConsumer<JsonElement, Object> consumer) {
      if (!this.TYPE_SAVERS.containsKey(type)) {
         this.TYPE_SAVERS.put(type, consumer);
      }
   }

   @Override
   public String getConfigFilePath() {
      return "lootbeams/config.json";
   }

   @Override
   public File getConfigFile() {
      return FabricLoader.getInstance().getConfigDir().resolve(this.getConfigFilePath()).toFile();
   }

   private JsonArray toArray(List<String> list) {
      JsonArray jsonArray = new JsonArray();

      for (String str : list) {
         jsonArray.add(new JsonPrimitive(str));
      }

      return jsonArray;
   }

   private List<String> toList(JsonArray jsonArray) {
      return jsonArray.asList().stream().<String>map(JsonElement::getAsString).toList();
   }

   @Override
   public void initialize(DynamicConfig.ConfigManager<Configuration> configManager) {
      this.registerTypeLoadConsumer(boolean.class, (field, group) -> GsonHelper.getAsBoolean(group, field.saveKey, (Boolean)field.defaultValue));
      this.registerTypeLoadConsumer(int.class, (field, group) -> GsonHelper.getAsInt(group, field.saveKey, (Integer)field.defaultValue));
      this.registerTypeLoadConsumer(float.class, (field, group) -> GsonHelper.getAsFloat(group, field.saveKey, (Float)field.defaultValue));
      this.registerTypeLoadConsumer(String.class, (field, group) -> GsonHelper.getAsString(group, field.saveKey, (String)field.defaultValue));
      this.registerTypeLoadConsumer(
         List.class, (field, group) -> this.toList(GsonHelper.getAsJsonArray(group, field.saveKey, this.toArray((List<String>)field.defaultValue)))
      );
      this.registerTypeSaveConsumer(boolean.class, value -> new JsonPrimitive((Boolean)value));
      this.registerTypeSaveConsumer(int.class, value -> new JsonPrimitive((Integer)value));
      this.registerTypeSaveConsumer(float.class, value -> new JsonPrimitive((Float)value));
      this.registerTypeSaveConsumer(String.class, value -> new JsonPrimitive((String)value));
      this.registerTypeSaveConsumer(List.class, value -> this.toArray((List<String>)value));
      this.registerTypeLoadConsumer(
         ParticleManager.ParticleTexture.class,
         (field, group) -> ParticleManager.ParticleTexture.of(
            GsonHelper.getAsString(group, field.saveKey, ((ParticleManager.ParticleTexture)field.defaultValue).path), "lootbeams"
         )
      );
      this.registerTypeSaveConsumer(ParticleManager.ParticleTexture.class, value -> new JsonPrimitive(((ParticleManager.ParticleTexture)value).path));
      this.registerTypeLoadConsumer(
         GlowEffectManager.GlowEffectTexture.class,
         (field, group) -> GlowEffectManager.GlowEffectTexture.of(
            GsonHelper.getAsString(group, field.saveKey, ((GlowEffectManager.GlowEffectTexture)field.defaultValue).path), "lootbeams"
         )
      );
      this.registerTypeSaveConsumer(GlowEffectManager.GlowEffectTexture.class, value -> new JsonPrimitive(((GlowEffectManager.GlowEffectTexture)value).path));
      this.registerTypeLoadConsumer(
         LootBeamShaders.Shader.class,
         (field, group) -> LootBeamShaders.Shader.valueOf(
            GsonHelper.getAsString(group, field.saveKey, ((LootBeamShaders.Shader)field.defaultValue).name()).toUpperCase()
         )
      );
      this.registerTypeSaveConsumer(LootBeamShaders.Shader.class, value -> new JsonPrimitive(((LootBeamShaders.Shader)value).name().toLowerCase()));
      this.registerTypeLoadConsumer(
         LootBeamShaders.CustomShader.class,
         (field, group) -> LootBeamShaders.CustomShader.valueOf(
            GsonHelper.getAsString(group, field.saveKey, ((LootBeamShaders.CustomShader)field.defaultValue).name()).toUpperCase()
         )
      );
      this.registerTypeSaveConsumer(LootBeamShaders.CustomShader.class, value -> new JsonPrimitive(((LootBeamShaders.CustomShader)value).name().toLowerCase()));
      this.registerTypeLoadConsumer(
         BeamOpacityOnApproach.class,
         (field, group) -> BeamOpacityOnApproach.valueOf(
            GsonHelper.getAsString(group, field.saveKey, ((BeamOpacityOnApproach)field.defaultValue).name()).toUpperCase()
         )
      );
      this.registerTypeSaveConsumer(BeamOpacityOnApproach.class, value -> new JsonPrimitive(((BeamOpacityOnApproach)value).name().toLowerCase()));
      this.registerTypeLoadConsumer(
         BeamSizeOnApproach.class,
         (field, group) -> BeamSizeOnApproach.valueOf(
            GsonHelper.getAsString(group, field.saveKey, ((BeamSizeOnApproach)field.defaultValue).name()).toUpperCase()
         )
      );
      this.registerTypeSaveConsumer(BeamSizeOnApproach.class, value -> new JsonPrimitive(((BeamSizeOnApproach)value).name().toLowerCase()));
   }

   public DynamicConfig.ConfigManager<Configuration> loadJson(DynamicConfig.ConfigManager<Configuration> configManager, JsonObject jsonObject) {
      if (jsonObject == null) {
         return configManager;
      } else {
         try {
            Map<String, JsonObject> groups = new HashMap<>();

            for (DynamicConfig.Control.Field field : configManager.getFields()) {
               if (!groups.containsKey(field.category.saveKey)) {
                  groups.put(field.category.saveKey, GsonHelper.getAsJsonObject(jsonObject, field.category.saveKey, new JsonObject()));
               }

               JsonObject group = groups.get(field.category.saveKey);
               if (group != null && !group.isJsonNull() && group.has(field.saveKey)) {
                  DynamicConfig.ConfigManager.LoadConsumer<Object, DynamicConfig.Control.Field, JsonObject> loadConsumer = this.TYPE_LOADERS.get(field.type);
                  if (loadConsumer != null) {
                     configManager.setFieldValue(field.key, loadConsumer.accept(field, group));
                  }
               }
            }
         } catch (Exception var8) {
            CrashManager.LOGGER.warn("cannot load config file", var8);
         }

         configManager.clearFieldValueCache();
         return configManager;
      }
   }

   private boolean migrateDistancesAfterLoad(DynamicConfig.ConfigManager<Configuration> configManager) {
      Configuration configuration = configManager.getConfig();
      if (configuration == null) {
         return false;
      } else {
         boolean migrated = configuration.migrateDistanceDefaults();
         if (migrated) {
            configManager.clearFieldValueCache();
         }

         return migrated;
      }
   }

   @Override
   public DynamicConfig.ConfigManager<Configuration> load(DynamicConfig.ConfigManager<Configuration> configManager, InputStream inputStream) {
      JsonObject jsonObject = GsonHelper.parse(new InputStreamReader(inputStream));
      DynamicConfig.ConfigManager<Configuration> loaded = this.loadJson(configManager, jsonObject);
      this.migrateDistancesAfterLoad(loaded);
      return loaded;
   }

   @Override
   public DynamicConfig.ConfigManager<Configuration> load(DynamicConfig.ConfigManager<Configuration> configManager, File configFile) {
      try {
         JsonObject jsonObject = GsonHelper.parse(new InputStreamReader(new FileInputStream(configFile), "UTF8"));
         DynamicConfig.ConfigManager<Configuration> loaded = this.loadJson(configManager, jsonObject);
         if (this.migrateDistancesAfterLoad(loaded)) {
            this.save(loaded, configFile);
         }

         return loaded;
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   @Override
   public DynamicConfigAdapter.SaveResult save(DynamicConfig.ConfigManager<Configuration> configManager, File saveFile) {
      JsonObject rootObject = new JsonObject();

      for (DynamicConfig.Control.Field field : configManager.getFields()) {
         DynamicConfig.ConfigManager.SaveConsumer<JsonElement, Object> saveConsumer = this.TYPE_SAVERS.get(field.type);
         if (saveConsumer != null) {
            try {
               Object value = configManager.getFieldValue(field.key);
               if (!rootObject.has(field.category.saveKey)) {
                  rootObject.add(field.category.saveKey, new JsonObject());
               }

               JsonObject category = rootObject.getAsJsonObject(field.category.saveKey);
               category.add(field.saveKey, saveConsumer.accept(value));
            } catch (Exception var12) {
               var12.printStackTrace();
            }
         }
      }

      saveFile = FileHelper.createPathIfNotExists(saveFile.getPath());

      try {
         DynamicConfigAdapter.SaveResult var16;
         try (FileWriter jsonWriter = new FileWriter(saveFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(rootObject, jsonWriter);
            var16 = DynamicConfigAdapter.SaveResult.SUCCESS;
         }

         return var16;
      } catch (IOException var11) {
         CrashManager.LOGGER.warn("Cannot save config file", var11);
         return DynamicConfigAdapter.SaveResult.FAIL;
      }
   }
}
