package com.lootbeams.dconfig;

import com.lootbeams.dconfig.events.ConfigEvents;
import com.lootbeams.dconfig.interfaces.DynamicConfigAdapter;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicConfig {
   public DynamicConfig() {
   }

   public static <T> DynamicConfig.ConfigManager<T> load(String modId, Class<T> configClass, DynamicConfigAdapter<T> configAdapter) {
      DynamicConfig.ConfigManager<T> configManager = new DynamicConfig.ConfigManager<>(modId, configClass, configAdapter);
      return configManager.load();
   }

   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Category {
      String name();

      String key();

      boolean root() default false;

      boolean display() default true;
   }

   public static class ConfigManager<T> {
      private String MOD_ID = "";
      private T config;
      private List<DynamicConfig.Control.Field> fields;
      private final DynamicConfigAdapter<T> adapter;
      private final Map<String, Object> FIELD_VALUE_CACHE = new HashMap<>();

      public ConfigManager(String modId, Class<T> configClass, DynamicConfigAdapter<T> adapter) {
         this.MOD_ID = modId;
         this.adapter = adapter;

         try {
            this.config = configClass.getDeclaredConstructor().newInstance();
            this.fields = this.loadFields(configClass);
         } catch (Exception var5) {
            var5.printStackTrace();
         }

         this.adapter.initialize(this);
      }

      public String getConfigFilePath() {
         return this.adapter.getConfigFilePath();
      }

      public <T> void printConfig(T config) throws IllegalAccessException {
         java.lang.reflect.Field[] classFields = config.getClass().getDeclaredFields();

         for (java.lang.reflect.Field field : classFields) {
            System.out.println(field.getName() + ": " + field.get(config));
         }
      }

      public void printFields() {
         for (DynamicConfig.Control.Field field : this.fields) {
            System.out.println(field);
         }
      }

      private <T> List<DynamicConfig.Control.Field> loadFields(Class<T> configClass) throws IllegalAccessException {
         Objects.requireNonNull(configClass);
         Map<String, DynamicConfig.Control.Category> categories = new HashMap<>();

         for (Class<?> innerClass : configClass.getDeclaredClasses()) {
            for (java.lang.reflect.Field field : innerClass.getDeclaredFields()) {
               if (field.isAnnotationPresent(DynamicConfig.Category.class)) {
                  DynamicConfig.Category categoryData = field.getAnnotation(DynamicConfig.Category.class);
                  String categoryKey = (String)field.get(null);
                  DynamicConfig.Control.Category category = new DynamicConfig.Control.Category(categoryKey, categoryData.name(), categoryData.key())
                     .setDisplayOnConfigScreen(categoryData.display())
                     .setAsRoot(categoryData.root());
                  categories.put(categoryKey, category);
               }
            }
         }

         List<DynamicConfig.Control.Field> fields = new ArrayList<>();
         java.lang.reflect.Field[] classFields = configClass.getDeclaredFields();

         for (java.lang.reflect.Field fieldx : classFields) {
            if (fieldx.isAnnotationPresent(DynamicConfig.Field.class)) {
               DynamicConfig.Field dynamicField = fieldx.getAnnotation(DynamicConfig.Field.class);
               String categoryKey = dynamicField.category();
               DynamicConfig.Control.Category category = categories.get(categoryKey);
               String group = dynamicField.group();
               boolean displayOnConfigScreen = dynamicField.display();
               Class<?> type = fieldx.getType();
               Type[] typeArguments = null;
               Object defaultValue = null;
               if (fieldx.getGenericType() instanceof ParameterizedType parameterizedType) {
                  typeArguments = parameterizedType.getActualTypeArguments();
               }

               try {
                  defaultValue = fieldx.get(this.config);
               } catch (IllegalAccessException var23) {
                  var23.printStackTrace();
               }

               DynamicConfig.Control.Field configField = new DynamicConfig.Control.Field(
                     this.MOD_ID, fieldx.getName(), category, type, typeArguments, defaultValue
                  )
                  .setGroup(group)
                  .setDisplayOnConfigScreen(displayOnConfigScreen);
               double minValue = dynamicField.min();
               double maxValue = dynamicField.max();
               if (type == int.class) {
                  configField.setMin((int)minValue);
                  configField.setMax((int)maxValue);
               } else if (type == float.class) {
                  configField.setMin((float)minValue);
                  configField.setMax((float)maxValue);
               } else if (type == double.class) {
                  configField.setMin(minValue);
                  configField.setMax(maxValue);
               } else if (type == long.class) {
                  configField.setMin((long)minValue);
                  configField.setMax((long)maxValue);
               }

               fields.add(configField);
            }
         }

         return fields;
      }

      public T getConfig() {
         return this.config;
      }

      public List<DynamicConfig.Control.Field> getFields() {
         return this.fields;
      }

      public DynamicConfig.Control.Field getField(String fieldKey) {
         return this.fields.stream().filter(field -> field.key.equals(fieldKey)).findFirst().orElse(null);
      }

      public List<DynamicConfig.Control.Field> getFieldsByCategory(String category) {
         List<DynamicConfig.Control.Field> fields = new ArrayList<>();

         for (DynamicConfig.Control.Field field : this.fields) {
            if (field.category.key.equals(category)) {
               fields.add(field);
            }
         }

         return fields;
      }

      public List<DynamicConfig.Control.Field> getFieldsByGroup(String group) {
         List<DynamicConfig.Control.Field> fields = new ArrayList<>();

         for (DynamicConfig.Control.Field field : this.fields) {
            if (field.group.equals(group)) {
               fields.add(field);
            }
         }

         return fields;
      }

      public List<DynamicConfig.Control.Field> getFieldsByCategoryAndGroup(String category, String group) {
         return this.getFieldsByCategory(category).stream().filter(field -> field.group.equals(group)).toList();
      }

      public void clearFieldValueCache() {
         this.FIELD_VALUE_CACHE.clear();
      }

      public <V> V getFieldValue(String fieldName) {
         if (this.FIELD_VALUE_CACHE.containsKey(fieldName)) {
            return (V)this.FIELD_VALUE_CACHE.get(fieldName);
         } else {
            try {
               java.lang.reflect.Field configField = this.config.getClass().getDeclaredField(fieldName);
               Object value = configField.get(this.config);
               this.FIELD_VALUE_CACHE.put(fieldName, value);
               return (V)value;
            } catch (Exception var4) {
               var4.printStackTrace();
               return null;
            }
         }
      }

      public void setFieldValue(String fieldName, Object value) {
         try {
            java.lang.reflect.Field configField = this.config.getClass().getDeclaredField(fieldName);
            configField.setAccessible(true);
            configField.set(this.config, value);
            if (this.FIELD_VALUE_CACHE.containsKey(fieldName)) {
               this.FIELD_VALUE_CACHE.put(fieldName, value);
            }
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }

      public DynamicConfig.ConfigManager<T> load(InputStream inputStream) {
         return this.adapter.load(this, inputStream);
      }

      public DynamicConfig.ConfigManager<T> load(File configFile) {
         if (!configFile.isFile()) {
            this.adapter.save(this, configFile);
         }

         return this.adapter.load(this, configFile);
      }

      public DynamicConfig.ConfigManager<T> load() {
         return this.load(this.adapter.getConfigFile());
      }

      public void save(File configFile) {
         DynamicConfigAdapter.SaveResult result = this.adapter.save(this, configFile);
         if (result == DynamicConfigAdapter.SaveResult.SUCCESS) {
            ConfigEvents.SAVE.invoker().onConfigSave();
         }
      }

      public void save() {
         this.save(this.adapter.getConfigFile());
      }

      @FunctionalInterface
      public interface LoadConsumer<T, U, V> {
         T accept(U var1, V var2);
      }

      @FunctionalInterface
      public interface SaveConsumer<T, U> {
         T accept(U var1);
      }
   }

   public static class Control {
      public Control() {
      }

      public static class Category {
         public String key;
         public String name;
         public String saveKey;
         public boolean displayOnConfigScreen = true;
         public boolean isRootCategory = false;

         public Category(String key, String name, String saveKey) {
            this.key = key;
            this.name = name;
            this.saveKey = saveKey;
         }

         public DynamicConfig.Control.Category setAsRoot(boolean isRootCategory) {
            this.isRootCategory = isRootCategory;
            return this;
         }

         public DynamicConfig.Control.Category setDisplayOnConfigScreen(boolean displayOnConfigScreen) {
            this.displayOnConfigScreen = displayOnConfigScreen;
            return this;
         }

         @Override
         public String toString() {
            return "DynamicConfig.Category[key="
               + this.key
               + ", name="
               + this.name
               + ", displayOnConfigScreen="
               + this.displayOnConfigScreen
               + ", isRootCategory="
               + this.isRootCategory
               + "]";
         }
      }

      public static class Field {
         public String key;
         public String saveKey;
         public DynamicConfig.Control.Category category;
         public String group = "default";
         public String name;
         public String description;
         public Class<?> type;
         public Type[] typeArguments;
         public Object defaultValue;
         public Object minValue = null;
         public Object maxValue = null;
         public boolean displayOnConfigScreen = true;

         public Field(String modId, String key, DynamicConfig.Control.Category category, Class<?> type, Type[] typeArguments, Object defaultValue) {
            this.key = key;
            this.saveKey = this.camelToSnake(key);
            this.category = category;
            this.name = MessageFormat.format("{0}.config.{1}.{2}", modId, category.key, key);
            this.description = MessageFormat.format("{0}.config.{1}.{2}.description", modId, category.key, key);
            this.type = type;
            this.typeArguments = typeArguments;
            this.defaultValue = defaultValue;
         }

         private String camelToSnake(String str) {
            return str.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2").toLowerCase();
         }

         public DynamicConfig.Control.Field setMin(Object minValue) {
            this.minValue = minValue;
            return this;
         }

         public DynamicConfig.Control.Field setMax(Object maxValue) {
            this.maxValue = maxValue;
            return this;
         }

         public DynamicConfig.Control.Field setGroup(String group) {
            this.group = group;
            return this;
         }

         public DynamicConfig.Control.Field setDisplayOnConfigScreen(boolean displayOnConfigScreen) {
            this.displayOnConfigScreen = displayOnConfigScreen;
            return this;
         }

         @Override
         public String toString() {
            return "DynamicConfig.Field[key="
               + this.key
               + ", saveKey="
               + this.saveKey
               + ", category="
               + this.category
               + ", name="
               + this.name
               + ", description="
               + this.description
               + ", type="
               + this.type
               + ", typeArguments="
               + this.typeArguments
               + ", defaultValue="
               + this.defaultValue
               + "]";
         }
      }

      public static class Group {
         public static final String DEFAULT = "default";

         public Group() {
         }
      }
   }

   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Field {
      String category();

      String group() default "default";

      double min() default 0.0;

      double max() default Double.MAX_VALUE;

      boolean display() default true;
   }
}
