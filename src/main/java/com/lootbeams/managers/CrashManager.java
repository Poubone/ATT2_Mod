package com.lootbeams.managers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CrashManager {
   public static final Logger LOGGER = LogManager.getLogger();
   public static List<ItemStack> CRASH_BLACKLIST = new ArrayList<>();

   public CrashManager() {
   }
}
