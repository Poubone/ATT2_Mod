package com.lootbeams.features;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lootbeams.LootBeams;
import com.lootbeams.compat.prism.PrismCompat;
import com.lootbeams.helpers.TextColorHelper;
import com.lootbeams.utils.Selectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class CustomRarity {
   private static final String CUSTOM_RARITIES_FILE = "custom_rarities.json";
   private static final String CUSTOM_RARITIES_JSON_KEY = "custom_rarities";
   private static final Map<String, CustomRarity> RARITY_CACHE = new HashMap<>();
   private static final String CUSTOM_RARITY_KEY = "custom_rarity";
   private static final String CUSTOM_RARITY_ID_KEY = "id";
   private static final String CUSTOM_RARITY_NAME_KEY = "name";
   private static final String CUSTOM_RARITY_COLOR_KEY = "color";
   private static final String CUSTOM_RARITY_ANIMATED_COLOR_KEY = "animated_color";
   private static final String CUSTOM_RARITY_CONFIG_KEY = "config";
   private static final String CUSTOM_RARITY_SELECTORS_KEY = "selectors";
   public static final String ITEM_CUSTOM_RARITY_ID_KEY = "custom_rarity_id";
   private String name = "";
   private int color = 16777215;
   private TextColor animatedColor = null;
   private JsonObject rarityConfig = null;
   private List<String> selectors = new ArrayList<>();

   public CustomRarity(String rarityName) {
      this.name = rarityName;
   }

   public CustomRarity setColor(int rarityColor) {
      this.color = rarityColor;
      return this;
   }

   public CustomRarity setAnimatedColor(TextColor prismColor) {
      this.animatedColor = prismColor;
      return this;
   }

   public CustomRarity setConfig(JsonObject rarityConfig) {
      this.rarityConfig = rarityConfig;
      return this;
   }

   public CustomRarity setSelectors(List<String> selectors) {
      this.selectors = selectors;
      return this;
   }

   public String getName() {
      return this.name;
   }

   public int getColor() {
      return this.animatedColor != null ? this.animatedColor.getValue() : this.color;
   }

   public boolean hasRarityConfig() {
      return this.rarityConfig != null;
   }

   public List<String> getSelectors() {
      return this.selectors;
   }

   public JsonObject getRarityConfig() {
      return this.rarityConfig;
   }

   public static CustomRarity fromSelectors(ItemStack itemStack) {
      for (CustomRarity rarity : RARITY_CACHE.values()) {
         if (!rarity.getSelectors().isEmpty() && Minecraft.getInstance().level != null) {
            for (String selector : rarity.getSelectors()) {
               if (Selectors.validateSelector(selector) && Selectors.itemMatches(itemStack, selector, Minecraft.getInstance().level.registryAccess())) {
                  return rarity;
               }
            }
         }
      }

      return null;
   }

   public static CustomRarity fromItemStack(ItemStack itemStack) {
      if (itemStack.getComponents().has(DataComponents.CUSTOM_DATA)) {
         CustomData itemCustomData = (CustomData)itemStack.get(DataComponents.CUSTOM_DATA);
         if (itemCustomData != null) {
            CompoundTag customDataNbt = itemCustomData.copyTag();
            if (!customDataNbt.isEmpty()) {
               CompoundTag customRarity = customDataNbt.getCompoundOrEmpty("custom_rarity");
               if (!customRarity.isEmpty()) {
                  String rarityName = customRarity.getStringOr("name", "");
                  String hexColor = customRarity.getStringOr("color", "");
                  String animatedHexColor = customRarity.getStringOr("animated_color", "");
                  if (!rarityName.isEmpty()) {
                     CustomRarity newRarity = new CustomRarity(Component.translatable(rarityName).getString());
                     if (!hexColor.isEmpty()) {
                        newRarity.setColor(TextColorHelper.getColorFromHEX(hexColor));
                     }

                     if (!animatedHexColor.isEmpty() && PrismCompat.isPrismLoaded()) {
                        TextColor prismColor = PrismCompat.parseColor(animatedHexColor);
                        newRarity.setAnimatedColor(prismColor);
                        newRarity.setColor(prismColor.getValue());
                     }

                     return newRarity;
                  }
               }

               String equipmentId = customDataNbt.getStringOr("EquipmentID", "");
               String customRarityId = customDataNbt.getStringOr("custom_rarity_id", "");
               // ESC drops are quartz tagged as an ATT2 coin and otherwise carry
               // Rarity=unk. The coin must win so this rare pickup does not use
               // the generic unknown-item beam.
               String coinId = customDataNbt.getStringOr("Coin", "");
               String att2Rarity = customDataNbt.getStringOr("Rarity", "");
               String lookupId = LootBeamIdResolver.firstCached(RARITY_CACHE,
                  equipmentId, customRarityId, coinId, att2Rarity);
               if (!lookupId.isEmpty()) {
                  return RARITY_CACHE.get(lookupId);
               }
            }
         }
      }

      CustomRarity rarity = fromSelectors(itemStack);
      return rarity != null ? rarity : null;
   }

   public static Component toText(CustomRarity customRarity) {
      Style newStyle = Style.EMPTY.withItalic(false).withColor(customRarity.getColor());
      return Component.literal(customRarity.getName()).setStyle(newStyle);
   }

   public static void onResourcesReload(ResourceManager resourceManager) {
      RARITY_CACHE.clear();
      List<Resource> files = resourceManager.getResourceStack(LootBeams.id("custom_rarities.json"));
      for (Resource customRaritiesFile : files) {
         try {
            JsonObject customRaritiesJSON = GsonHelper.parse(customRaritiesFile.openAsReader());
            if (!customRaritiesJSON.has("custom_rarities")) {
               continue;
            }
            JsonArray customRarities = GsonHelper.getAsJsonArray(customRaritiesJSON, "custom_rarities");
            for (JsonElement jsonElement : customRarities) {
               if (jsonElement.isJsonObject()) {
                  parseAndPut(jsonElement.getAsJsonObject());
               }
            }
         } catch (Exception exception) {
            exception.printStackTrace();
         }
      }
   }

   private static void parseAndPut(JsonObject jsonObject) {
      if (!jsonObject.has("id")) {
         return;
      }
      String rarityId = GsonHelper.getAsString(jsonObject, "id");
      String rarityName = GsonHelper.getAsString(jsonObject, "name");
      String rarityColor = GsonHelper.getAsString(jsonObject, "color");
      String rarityAnimatedColor = GsonHelper.getAsString(jsonObject, "animated_color", "");
      JsonArray raritySelectors = GsonHelper.getAsJsonArray(jsonObject, "selectors", new JsonArray());
      JsonObject rarityConfig = GsonHelper.getAsJsonObject(jsonObject, "config", new JsonObject());
      if (rarityId.isEmpty() || rarityName.isEmpty()) {
         return;
      }
      CustomRarity newRarity = new CustomRarity(Component.translatable(rarityName).getString());
      if (!rarityColor.isEmpty()) {
         newRarity.setColor(TextColorHelper.getColorFromHEX(rarityColor));
      }
      if (!rarityAnimatedColor.isEmpty() && PrismCompat.isPrismLoaded()) {
         TextColor prismColor = PrismCompat.parseColor(rarityAnimatedColor);
         newRarity.setAnimatedColor(prismColor);
         newRarity.setColor(prismColor.getValue());
      }
      if (!raritySelectors.isEmpty()) {
         List<String> selectors = new ArrayList<>();
         for (JsonElement selectorElement : raritySelectors) {
            selectors.add(GsonHelper.convertToString(selectorElement, "selector"));
         }
         if (!selectors.isEmpty()) {
            newRarity.setSelectors(selectors);
         }
      }
      if (!rarityConfig.isEmpty()) {
         newRarity.setConfig(rarityConfig);
      }
      RARITY_CACHE.put(rarityId, newRarity);
   }

   @Override
   public String toString() {
      return this.getClass().getName() + "[name=" + this.name + ", color=" + this.color + ", rarityConfig=" + this.rarityConfig + "]";
   }
}
