package com.lootbeams.features;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lootbeams.LootBeams;
import com.lootbeams.config.Configuration;
import com.lootbeams.dconfig.DynamicConfig;
import com.lootbeams.managers.GlowEffectManager;
import com.lootbeams.managers.ParticleManager;
import com.lootbeams.shaders.LootBeamShaders;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class CustomLootBeamsConfig {
   private static final String CONFIG_KEY = "lootbeams_config";
   private static final Map<ItemStack, Configuration> CONFIG_CACHE = new WeakHashMap<>();

   public CustomLootBeamsConfig() {
   }

   public static void clearConfigCache() {
      CONFIG_CACHE.clear();
   }

   public static void onConfigurationChange() {
      clearConfigCache();
   }

   public static void onResourcesReload() {
      clearConfigCache();
   }

   private static Configuration getConfigCopy(Configuration configInstance) {
      Configuration configCopy = new Configuration();

      for (Field field : configInstance.getClass().getDeclaredFields()) {
         try {
            Field fieldRef = configCopy.getClass().getDeclaredField(field.getName());
            fieldRef.setAccessible(true);
            fieldRef.set(configCopy, field.get(configInstance));
         } catch (Exception var7) {
            var7.printStackTrace();
         }
      }

      return configCopy;
   }

   private static Configuration processConfig(CompoundTag customConfig) {
      Configuration configCopy = getConfigCopy(LootBeams.config);

      for (DynamicConfig.Control.Field field : LootBeams.configManager.getFieldsByGroup("visual")) {
         try {
            Field fieldRef = configCopy.getClass().getDeclaredField(field.key);
            fieldRef.setAccessible(true);
            if (!customConfig.isEmpty() && customConfig.contains(field.saveKey)) {
               if (field.type == boolean.class) {
                  fieldRef.set(configCopy, customConfig.getBooleanOr(field.saveKey, false));
               }

               if (field.type == int.class) {
                  fieldRef.set(configCopy, customConfig.getIntOr(field.saveKey, 0));
               }

               if (field.type == long.class) {
                  fieldRef.set(configCopy, customConfig.getLongOr(field.saveKey, 0L));
               }

               if (field.type == float.class) {
                  fieldRef.set(configCopy, customConfig.getFloatOr(field.saveKey, 0.0F));
               }

               if (field.type == double.class) {
                  fieldRef.set(configCopy, customConfig.getDoubleOr(field.saveKey, 0.0));
               }

               if (field.type == String.class) {
                  fieldRef.set(configCopy, customConfig.getStringOr(field.saveKey, ""));
               }

               if (field.type == List.class) {
                  String value = customConfig.getStringOr(field.saveKey, "");
                  List<String> values = Arrays.stream(value.split(",")).map(String::trim).toList();
                  fieldRef.set(configCopy, values);
               }

               if (field.type == ParticleManager.ParticleTexture.class) {
                  String textureKey = customConfig.getStringOr(field.saveKey, "");
                  if (textureKey.contains(":")) {
                     String[] parts = textureKey.split(":");
                     fieldRef.set(configCopy, ParticleManager.ParticleTexture.of(textureKey, parts[0]));
                  } else {
                     fieldRef.set(configCopy, ParticleManager.ParticleTexture.of(textureKey, "lootbeams"));
                  }
               }

               if (field.type == GlowEffectManager.GlowEffectTexture.class) {
                  String textureKey = customConfig.getStringOr(field.saveKey, "");
                  if (textureKey.contains(":")) {
                     String[] parts = textureKey.split(":");
                     fieldRef.set(configCopy, GlowEffectManager.GlowEffectTexture.of(textureKey, parts[0]));
                  } else {
                     fieldRef.set(configCopy, GlowEffectManager.GlowEffectTexture.of(textureKey, "lootbeams"));
                  }
               }

               if (field.type == LootBeamShaders.Shader.class) {
                  fieldRef.set(configCopy, LootBeamShaders.Shader.valueOf(customConfig.getStringOr(field.saveKey, "").toUpperCase()));
               }

               if (field.type == LootBeamShaders.CustomShader.class) {
                  fieldRef.set(configCopy, LootBeamShaders.CustomShader.valueOf(customConfig.getStringOr(field.saveKey, "").toUpperCase()));
               }
            } else {
               fieldRef.set(configCopy, LootBeams.configManager.getFieldValue(field.key));
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }
      }

      return configCopy;
   }

   private static Configuration processConfig(JsonObject customConfig) {
      Configuration configCopy = getConfigCopy(LootBeams.config);

      for (DynamicConfig.Control.Field field : LootBeams.configManager.getFieldsByGroup("visual")) {
         try {
            Field fieldRef = configCopy.getClass().getDeclaredField(field.key);
            fieldRef.setAccessible(true);
            if (!customConfig.isEmpty() && customConfig.has(field.saveKey)) {
               if (field.type == boolean.class) {
                  fieldRef.set(configCopy, GsonHelper.getAsBoolean(customConfig, field.saveKey));
               }

               if (field.type == int.class) {
                  fieldRef.set(configCopy, GsonHelper.getAsInt(customConfig, field.saveKey));
               }

               if (field.type == long.class) {
                  fieldRef.set(configCopy, GsonHelper.getAsLong(customConfig, field.saveKey));
               }

               if (field.type == float.class) {
                  fieldRef.set(configCopy, GsonHelper.getAsFloat(customConfig, field.saveKey));
               }

               if (field.type == double.class) {
                  fieldRef.set(configCopy, GsonHelper.getAsDouble(customConfig, field.saveKey));
               }

               if (field.type == String.class) {
                  fieldRef.set(configCopy, GsonHelper.getAsString(customConfig, field.saveKey));
               }

               if (field.type == List.class) {
                  List<String> values = GsonHelper.getAsJsonArray(customConfig, field.saveKey).asList().stream().<String>map(JsonElement::getAsString).toList();
                  fieldRef.set(configCopy, values);
               }

               if (field.type == ParticleManager.ParticleTexture.class) {
                  String textureKey = GsonHelper.getAsString(customConfig, field.saveKey);
                  if (textureKey.contains(":")) {
                     String[] parts = textureKey.split(":");
                     fieldRef.set(configCopy, ParticleManager.ParticleTexture.of(textureKey, parts[0]));
                  } else {
                     fieldRef.set(configCopy, ParticleManager.ParticleTexture.of(textureKey, "lootbeams"));
                  }
               }

               if (field.type == GlowEffectManager.GlowEffectTexture.class) {
                  String textureKey = GsonHelper.getAsString(customConfig, field.saveKey);
                  if (textureKey.contains(":")) {
                     String[] parts = textureKey.split(":");
                     fieldRef.set(configCopy, GlowEffectManager.GlowEffectTexture.of(textureKey, parts[0]));
                  } else {
                     fieldRef.set(configCopy, GlowEffectManager.GlowEffectTexture.of(textureKey, "lootbeams"));
                  }
               }

               if (field.type == LootBeamShaders.Shader.class) {
                  fieldRef.set(configCopy, LootBeamShaders.Shader.valueOf(GsonHelper.getAsString(customConfig, field.saveKey).toUpperCase()));
               }

               if (field.type == LootBeamShaders.CustomShader.class) {
                  fieldRef.set(configCopy, LootBeamShaders.CustomShader.valueOf(GsonHelper.getAsString(customConfig, field.saveKey).toUpperCase()));
               }
            } else {
               fieldRef.set(configCopy, LootBeams.configManager.getFieldValue(field.key));
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }
      }

      return configCopy;
   }

   public static Configuration fromItemStack(ItemStack itemStack) {
      if (CONFIG_CACHE.containsKey(itemStack)) {
         return CONFIG_CACHE.get(itemStack);
      } else {
         if (itemStack.getComponents().has(DataComponents.CUSTOM_DATA)) {
            CustomData itemCustomData = (CustomData)itemStack.get(DataComponents.CUSTOM_DATA);
            if (itemCustomData != null) {
               CompoundTag customDataNbt = itemCustomData.copyTag();
               if (!customDataNbt.isEmpty()) {
                  CompoundTag customConfig = customDataNbt.getCompoundOrEmpty("lootbeams_config");
                  if (!customConfig.isEmpty()) {
                     Configuration newConfig = processConfig(customConfig);
                     CONFIG_CACHE.put(itemStack, newConfig);
                     return newConfig;
                  }
               }
            }
         }

         CustomRarity itemRarity = CustomRarity.fromItemStack(itemStack);
         if (itemRarity != null && itemRarity.hasRarityConfig()) {
            Configuration newConfig = processConfig(itemRarity.getRarityConfig());
            CONFIG_CACHE.put(itemStack, newConfig);
            return newConfig;
         } else {
            return LootBeams.config;
         }
      }
   }
}
