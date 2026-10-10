package fr.poubone.att2.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lootbeams.helpers.ItemHelper;
import fr.poubone.att2.client.renderer.ItemRarity;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HUDConfig {
    // HUD options
    public boolean showChronoton = true;
    public boolean showXP = true;
    public boolean showMana = true;
    public boolean showStats = true;
    public boolean showArrows = true;
    public boolean showArmorDurability = true;
    public boolean showSpellBar = true;
    public boolean showTemperature = true;
    /** Shade the Dahal and temperature orbs on the GPU; off uses the original CPU path. */
    public boolean orbGpuRendering = true;
    /**
     * Orb liquid redraws per second, one of {@link #ORB_FPS_STEPS} (0 redraws every frame), or
     * {@link #ORB_FPS_AUTO} until the slider is moved. See {@link #effectiveOrbFps()}.
     */
    public int orbFps = ORB_FPS_AUTO;
    /** Movable / resizable HUD boxes, keyed by {@link HudLayout} ids. */
    public Map<String, HudSlot> slots = new LinkedHashMap<>();
    /** Ask for a second key press before dropping an item whose custom_data.Rarity is listed here. */
    public boolean dropLockEnabled = true;
    public List<String> dropLockRarities = new ArrayList<>(List.of("myt", "que"));

    /** Discord Rich Presence (local IPC). Empty application id means "not configured yet". */
    public boolean discordRichPresence = true;
    /** Show the last quest opened in the book as the Discord presence state line. */
    public boolean discordShowQuest = true;
    /** Swap the large Rich Presence image for the current map region (portal art-asset key). */
    public boolean discordRegionImage = true;
    public String discordApplicationId = "1545088290577981521";
    public String discordMapUrl = "https://adventquest.com/across-the-time-ii-time-for-regrets";
    public String discordModUrl = "https://github.com/Poubone/ATT2_Project";
    /** Fallback art-asset key when the region image is unknown or not uploaded yet. */
    public String discordLargeImage = "logo-brillant";
    /**
     * Art-asset keys actually uploaded on the Discord portal. A region picture is only sent
     * when its key is listed here (or {@code *} to allow every region). Unknown keys make
     * Discord hide the image, so the default is just the logo already on the portal.
     */
    public List<String> discordUploadedAssets = new ArrayList<>(List.of("logo-brillant"));

    private static final String DEFAULT_DISCORD_APP_ID = "1545088290577981521";
    private static final String DEFAULT_DISCORD_MAP_URL = "https://adventquest.com/across-the-time-ii-time-for-regrets";
    private static final String DEFAULT_DISCORD_MOD_URL = "https://github.com/Poubone/ATT2_Project";
    private static final String DEFAULT_DISCORD_LARGE_IMAGE = "logo-brillant";

    public String modLanguage = "fr";

    /**
     * Base URL of the party-sync VPS (no trailing slash). Empty disables the API:
     * item share falls back to the datapack / tellraw path, pings stay off.
     */
    public volatile boolean partySyncEnabled = true;
    public String partySyncUrl = "https://sync.guide-att2.com";
    /** Legacy optional field; no token is needed by the public party-sync API. */
    public String partySyncToken = "";

    /** Values of the {@code custom_data.Rarity} string used by the map's items. */
    public List<String> renderRarities = new ArrayList<>(List.of(
            "com", "cur", "epi", "epi_set", "leg", "leg_armset",
            "misc", "myt", "que", "rar", "spe", "ult", "unc", "unk", "epi_esc", "esc"
    ));

    public boolean allItems = true;
    public boolean onlyEquipment = false;
    public boolean onlyRare = false;
    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>();
    public List<String> colorOverrides = new ArrayList<>();

    public boolean renderNameColor = true;
    public boolean renderRarityColor = true;
    public float beamRadius = 1;
    public float beamHeight = 1;
    public float beamYOffset = 0;
    public float beamAlpha = 0.85f;
    public float renderDistance = 24.0f;
    public float fadeDistance = 2.0f;

    public boolean borders = true;
    public boolean renderNametags = true;
    public boolean renderNametagsOnlook = true;
    public boolean renderStackcount = true;
    public float nametagLookSensitivity = 0.018f;
    public float nametagTextAlpha = 1;
    public float nametagBackgroundAlpha = 0.5f;
    public float nametagScale = 1.0f;
    public float nametagYOffset = 0.75f;
    public List<String> alwaysDrawRaritiesOn = List.of("#minecraft:music_discs");
    public boolean whiteRarities = false;

    // Quest book
    public boolean questBookShowMain = true;
    public boolean questBookShowSide = true;
    public boolean questBookShowCompleted = true;
    public boolean questBookShowDaily = true;
    /** Open Charles' counter automatically when the NPC offers his games in the chat. */
    public boolean charlesAutoOpen = true;
    /** Holding Sneak while talking to an NPC keeps the map's chat menu instead of opening the mod's window. */
    public boolean sneakSkipsMenus = true;
    public boolean runeMenuEnabled = true;
    public boolean minerMenuEnabled = true;
    public boolean questMenuEnabled = true;
    /**
     * Legacy master switch. When {@link #shopMenus} is absent from the config file,
     * {@code false} turns every stall off; otherwise each stall starts enabled.
     */
    public boolean shopAutoOpen = true;
    /**
     * Per-stall shop menus, keyed by {@link fr.poubone.att2.client.shop.ShopType#id}.
     * {@code null} until {@link #load()} migrates an older config.
     */
    public Map<String, Boolean> shopMenus = null;
    /** Spell stalls hide spells already learned or carried, and enhancements already maxed (toggled in the stall). */
    public boolean shopHideOwnedSpells = true;

    public static final List<String> SHOP_MENU_IDS = List.of(
            "blacksmith", "food", "alchemist", "fletcher", "dahal",
            "tailor", "fish", "stable", "general");
    /**
     * Texture set of the quest book: files are read from {@code assets/att2/textures/quest_book/<theme>/}.
     * The default "minecraft" uses game resources; other names select a resource-pack theme.
     */
    public String questBookTheme = "minecraft";
    /** Colours of the quest book, as #RRGGBB strings. */
    public String questBookTitleColor = "#FFFF55";
    public String questBookTextColor = "#000000";
    public String questBookSecondaryTextColor = "#404040";
    public String questBookEntryColor = "#B5AE97";
    public String questBookEntryHoverColor = "#797465";
    public String questBookSelectedColor = "#FFCE7F";
    public String questBookSelectedHoverColor = "#FFC432";
    public String questBookFilterColor = "#B5AE97";
    public String questBookFilterHoverColor = "#797465";
    public String questBookFilterEnabledColor = "#A4D48E";

    /** Parses a #RRGGBB / #AARRGGBB colour string into an ARGB int (opaque when no alpha is given). */
    public static int color(String value, int fallback) {
        if (value == null) return fallback;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "att2_hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HUDConfig INSTANCE;

    public static HUDConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    /** Orb animation rates offered by the settings slider; 0 means every frame. */
    public static final int[] ORB_FPS_STEPS = {15, 30, 60, 120, 0};
    /** No rate chosen yet: every frame on the GPU, 30 on the CPU. */
    public static final int ORB_FPS_AUTO = -1;

    /** True when the orbs actually render on the GPU: the setting is on and the shader compiled. */
    public boolean orbsOnGpu() {
        return orbGpuRendering && GlobeShader.isAvailable();
    }

    /**
     * Redraw rate in use. Until a rate is chosen, every frame when the GPU draws the orbs (nearly free)
     * and 30 a second on the CPU, where each redraw costs several milliseconds.
     */
    public int effectiveOrbFps() {
        if (orbFps != ORB_FPS_AUTO) return orbFps;
        return orbsOnGpu() ? 0 : 30;
    }

    public static boolean isValidOrbFps(int fps) {
        if (fps == ORB_FPS_AUTO) return true;
        for (int step : ORB_FPS_STEPS) {
            if (step == fps) return true;
        }
        return false;
    }

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader reader = new FileReader(FILE, java.nio.charset.StandardCharsets.UTF_8)) {
                    INSTANCE = GSON.fromJson(reader, HUDConfig.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (INSTANCE == null) INSTANCE = new HUDConfig();
        if (INSTANCE.renderRarities == null) {
            INSTANCE.renderRarities = new ArrayList<>(List.of(
                    "com", "cur", "epi", "epi_set", "leg", "leg_armset",
                    "misc", "myt", "que", "rar", "spe", "ult", "unc", "unk", "epi_esc", "esc"));
        } else if (!INSTANCE.renderRarities.contains("esc")) {
            // ESC had no individual toggle in earlier versions, so enable the new
            // rare-drop beam for existing configurations as well.
            INSTANCE.renderRarities.add("esc");
        }
        if (INSTANCE.whitelist == null) INSTANCE.whitelist = new ArrayList<>();
        if (INSTANCE.blacklist == null) INSTANCE.blacklist = new ArrayList<>();
        if (INSTANCE.slots == null) INSTANCE.slots = new LinkedHashMap<>();
        if (INSTANCE.slots != null) {
            for (HudSlot slot : INSTANCE.slots.values()) {
                if (slot == null) continue;
                if (slot.scale <= 0f) slot.scale = 1f;
                if (slot.gap <= 0f) slot.gap = 1f;
            }
        }
        if (INSTANCE.discordUploadedAssets == null) {
            INSTANCE.discordUploadedAssets = new ArrayList<>(List.of("logo-brillant"));
        }
        INSTANCE.modLanguage = ModLanguageManager.normalize(INSTANCE.modLanguage);
        // Gson leaves missing string fields as null on some paths; buttons need real URLs.
        if (INSTANCE.discordApplicationId == null || INSTANCE.discordApplicationId.isBlank()) {
            INSTANCE.discordApplicationId = DEFAULT_DISCORD_APP_ID;
        }
        if (INSTANCE.discordMapUrl == null || INSTANCE.discordMapUrl.isBlank()) {
            INSTANCE.discordMapUrl = DEFAULT_DISCORD_MAP_URL;
        }
        if (INSTANCE.discordModUrl == null || INSTANCE.discordModUrl.isBlank()) {
            INSTANCE.discordModUrl = DEFAULT_DISCORD_MOD_URL;
        }
        if (INSTANCE.discordLargeImage == null || INSTANCE.discordLargeImage.isBlank()) {
            INSTANCE.discordLargeImage = DEFAULT_DISCORD_LARGE_IMAGE;
        }
        if (!isValidOrbFps(INSTANCE.orbFps)) {
            INSTANCE.orbFps = ORB_FPS_AUTO;
        }

        if (INSTANCE.partySyncUrl == null) {
            INSTANCE.partySyncUrl = "";
        }
        if (INSTANCE.partySyncToken == null) {
            INSTANCE.partySyncToken = "";
        }
        if (INSTANCE.shopMenus == null) {
            INSTANCE.shopMenus = new LinkedHashMap<>();
            for (String id : SHOP_MENU_IDS) {
                INSTANCE.shopMenus.put(id, INSTANCE.shopAutoOpen);
            }
        } else {
            for (String id : SHOP_MENU_IDS) {
                INSTANCE.shopMenus.putIfAbsent(id, true);
            }
        }
    }

    public boolean isShopMenuEnabled(String id) {
        if (shopMenus == null || id == null) return shopAutoOpen;
        return shopMenus.getOrDefault(id, true);
    }

    public void setShopMenuEnabled(String id, boolean enabled) {
        if (shopMenus == null) shopMenus = new LinkedHashMap<>();
        shopMenus.put(id, enabled);
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE, java.nio.charset.StandardCharsets.UTF_8)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setModLanguage(String langCode) {
        get().modLanguage = ModLanguageManager.normalize(langCode);
        save();
    }

    public static String getModLanguage() {
        return get().modLanguage;
    }

    /** Colour override configured for an item (or null). */
    public static TextColor getColorFromItemOverrides(Item item) {
        List<String> overrides = get().colorOverrides;
        if (overrides.isEmpty()) return null;

        for (String unparsed : overrides.stream().filter(s -> !s.isEmpty()).toList()) {
            String[] configValue = unparsed.split("=");
            if (configValue.length != 2) continue;

            String nameIn = configValue[0];
            Optional<TextColor> colorIn = TextColor.parseColor(configValue[1]).result();
            if (colorIn.isEmpty()) return null;

            if (matchesItem(nameIn, item)) {
                return colorIn.get();
            }
        }
        return null;
    }

    /**
     * Whether the ATT2 HUD loot options allow a beam / glow / particles on this stack.
     * Matches the pre-fork filter: {@code allItems} (or equipment / rare / whitelist), not blacklisted,
     * and {@code custom_data.Rarity} listed in {@link #renderRarities}.
     */
    public static boolean shouldRenderLootBeam(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        HUDConfig config = get();
        Item item = stack.getItem();
        boolean whitelistMatch = matchesAnyList(config.whitelist, item);
        boolean blacklistMatch = matchesAnyList(config.blacklist, item);
        boolean itemMatch = config.allItems
                || (config.onlyEquipment && ItemHelper.isEquipmentItem(stack))
                || (config.onlyRare && stack.getRarity() != Rarity.COMMON)
                || whitelistMatch;
        if (!itemMatch || blacklistMatch) {
            return false;
        }
        String rarityId = lootRarityId(stack);
        return rarityId != null && config.renderRarities != null && config.renderRarities.contains(rarityId);
    }

    private static String lootRarityId(ItemStack stack) {
        ItemRarity rarity = ItemRarity.fromStack(stack);
        if (rarity != null) {
            return rarity.id;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }
        String coin = data.copyTag().getString("Coin").orElse("");
        return coin.equals("esc") ? coin : data.copyTag().getString("Rarity").orElse(null);
    }

    private static boolean matchesAnyList(List<String> entries, Item item) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (String entry : entries) {
            if (matchesItem(entry, item)) {
                return true;
            }
        }
        return false;
    }

    /** Matches "namespace", "namespace:item" or "#namespace:tag" entries against an item. */
    public static boolean matchesItem(String name, Item item) {
        if (name == null || name.isEmpty()) return false;
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);

        if (!name.contains(":")) {
            return itemId.getNamespace().equals(name);
        }

        Identifier id = Identifier.tryParse(name.replace("#", ""));
        if (id == null) return false;

        if (name.startsWith("#")) {
            Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id));
            return tag.isPresent() && tag.get().contains(item.builtInRegistryHolder());
        }

        return itemId.equals(id);
    }
}
