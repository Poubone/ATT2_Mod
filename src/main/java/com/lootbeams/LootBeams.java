package com.lootbeams;

import com.lootbeams.compat.iceberg.IcebergCompat;
import com.lootbeams.config.Configuration;
import com.lootbeams.config.JsonConfigAdapter;
import com.lootbeams.dconfig.DynamicConfig;
import com.lootbeams.managers.CrashManager;
import com.lootbeams.managers.ParticleManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;

public class LootBeams implements ClientModInitializer {
   public static final String MODID = "lootbeams";
   public static DynamicConfig.ConfigManager<Configuration> configManager;
   public static Configuration config;

   public LootBeams() {
      configManager = DynamicConfig.load("lootbeams", Configuration.class, new JsonConfigAdapter());
      config = configManager.getConfig();
      if (IcebergCompat.isIcebergLoaded()) {
         System.out.println(IcebergCompat.getPlatformName());
      }
   }

   public static Identifier id(String name) {
      return Identifier.fromNamespaceAndPath("lootbeams", name);
   }

   public void onInitializeClient() {
      ClientSetup.registerCoreShaderRegistrationEvents();
      ClientSetup.registerHudRenderEvents();
      ClientSetup.registerWorldRenderEvents();
      ClientSetup.registerCommands();
      ClientSetup.registerKeyBindings();
      ClientSetup.registerClientEvents();
      ClientSetup.registerCustomEvents();
      ParticleManager.registerParticles();
      CrashManager.LOGGER.info(
            "Loot beams: droplight={}, radius={}, height={}, particles={}, trails={}",
            config.renderDroplightBeam,
            config.beamRadius,
            config.beamHeight,
            config.particles,
            config.trails);
   }
}
