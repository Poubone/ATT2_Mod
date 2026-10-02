package com.lootbeams.dconfig.interfaces;

import com.lootbeams.dconfig.DynamicConfig;
import java.io.File;
import java.io.InputStream;

public interface DynamicConfigAdapter<T> {
   String getConfigFilePath();

   File getConfigFile();

   void initialize(DynamicConfig.ConfigManager<T> var1);

   DynamicConfigAdapter.SaveResult save(DynamicConfig.ConfigManager<T> var1, File var2);

   DynamicConfig.ConfigManager<T> load(DynamicConfig.ConfigManager<T> var1, File var2);

   DynamicConfig.ConfigManager<T> load(DynamicConfig.ConfigManager<T> var1, InputStream var2);

   public static enum SaveResult {
      SUCCESS,
      FAIL;

      private SaveResult() {
      }
   }
}
