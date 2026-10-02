package fr.poubone.att2.client.input;

import fr.poubone.att2.client.compat.FlashbackCompat;
import com.mojang.blaze3d.platform.InputConstants;
import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.SpellLauncherTracker;
import fr.poubone.att2.client.data.SpellSelectTriggers;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestBookScreen;
import fr.poubone.att2.client.screen.HUDConfigScreen;
import fr.poubone.att2.client.screen.SpellLevelRadialScreen;
import fr.poubone.att2.client.screen.RepairMenuScreen;
import fr.poubone.att2.client.screen.StatUpgradeScreen;
import fr.poubone.att2.client.sync.PartySync;
import fr.poubone.att2.client.util.BroadcastScanner;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KeybindManager {
    public static boolean showCustomHUD = true;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("att2", "att2"));

    private static KeyMapping toggleHUDKey;
    private static KeyMapping statUpgradeMenuKey;
    private static KeyMapping playerGlow;
    private static KeyMapping collectItems;
    private static KeyMapping questBook;
    private static KeyMapping repairItemMenuKey;
    private static boolean repairReleased = true;
    private static KeyMapping whistle;
    private static KeyMapping openHUDConfig;
    private static KeyMapping broadcastKey;
    private static KeyMapping pingKey;
    private static KeyMapping compareShopKey;
    private static KeyMapping weaponSkillKey;
    private static KeyMapping spellLevelMenuKey;
    private static boolean spellLevelReleased = true;

    private static boolean languageLoaded = false;

    private static long lastUsedTime = 0;
    private static final long COOLDOWN_MS = 5000;
    private static long lastPingTime = 0;
    private static final long PING_COOLDOWN_MS = 250;

    private static KeyMapping key(String name, int glfwKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.att2." + name, InputConstants.Type.KEYSYM, glfwKey, CATEGORY));
    }

    public static void register() {
        toggleHUDKey = key("toggle_hud", GLFW.GLFW_KEY_H);
        statUpgradeMenuKey = key("stat_upgrade_menu", GLFW.GLFW_KEY_U);
        playerGlow = key("player_glow", GLFW.GLFW_KEY_G);
        collectItems = key("collect_items", GLFW.GLFW_KEY_C);
        questBook = key("quest_main", GLFW.GLFW_KEY_B);
        repairItemMenuKey = key("repair_menu", GLFW.GLFW_KEY_I);
        whistle = key("whistle", GLFW.GLFW_KEY_V);
        openHUDConfig = key("hud_config", GLFW.GLFW_KEY_K);
        broadcastKey = key("broadcast", GLFW.GLFW_KEY_O);
        pingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.att2.ping", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, CATEGORY));
        compareShopKey = key("compare_shop", GLFW.GLFW_KEY_LEFT_SHIFT);
        weaponSkillKey = key("weapon_skill", InputConstants.UNKNOWN.getValue());
        spellLevelMenuKey = key("spell_level_menu", InputConstants.UNKNOWN.getValue());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (!languageLoaded && client.getResourceManager() != null) {
                languageLoaded = true;
                ModLanguageManager.loadLanguage(client, HUDConfig.getModLanguage());
            }

            while (toggleHUDKey.consumeClick()) {
                showCustomHUD = !showCustomHUD;
                String key = showCustomHUD ? "keybind.toggle_hud.on" : "keybind.toggle_hud.off";
                client.player.displayClientMessage(ModLanguageManager.get(key), true);
            }
            while (statUpgradeMenuKey.consumeClick()) {
                client.setScreen(new StatUpgradeScreen());
            }
            while (playerGlow.consumeClick()) {
                Att2Triggers.send(Att2Triggers.PLAYER_GLOW);
            }
            while (collectItems.consumeClick()) {
                Att2Triggers.send(Att2Triggers.COLLECT_ITEMS);
            }
            while (questBook.consumeClick()) {
                if (client.screen == null) {
                    QuestBookScreen.open();
                }
            }
            while (whistle.consumeClick()) {
                Att2Triggers.send(Att2Triggers.HORSE_WHISTLE);
            }
            while (openHUDConfig.consumeClick()) {
                client.setScreen(new HUDConfigScreen());
            }
            while (broadcastKey.consumeClick()) {
                handleBroadcastKey(client);
            }
            while (pingKey.consumeClick()) {
                handlePingKey(client);
            }
            while (weaponSkillKey.consumeClick()) {
                if (client.player != null && client.screen == null && !InputSequencer.isBusy()) {
                    triggerWeaponSkill(client);
                }
            }

            if (repairItemMenuKey.isDown()) {
                if (repairReleased && client.screen == null) {
                    client.setScreen(new RepairMenuScreen());
                    repairReleased = false;
                }
            } else {
                repairReleased = true;
            }

            if (spellLevelMenuKey.isDown()) {
                if (spellLevelReleased && client.screen == null) {
                    SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(client.player.getMainHandItem());
                    if (launcher != null && SpellSelectTriggers.isSupported(launcher.spellId)) {
                        client.setScreen(new SpellLevelRadialScreen(launcher));
                        spellLevelReleased = false;
                    }
                }
            } else {
                spellLevelReleased = true;
            }

            BroadcastScanner.tick(client);
        });
    }

    /**
     * One contextual key for the map's weapon combos. Priority: blocking with a shield ->
     * sword & shield combo (forced sneak + held right click); axe in the main hand -> throw it
     * (the map maps the throw to the swap-hands key); anything else -> a brief crouch, which
     * triggers the map's crouch skills (Backstab, Dahal Burst).
     */
    private static void triggerWeaponSkill(Minecraft client) {
        if (client.player.isBlocking()) {
            InputSequencer.run(List.of(
                    InputSequencer.Step.forceSneak(8),
                    InputSequencer.Step.wait(2),
                    InputSequencer.Step.holdUse(4)));
        } else if (client.player.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES)) {
            InputSequencer.run(List.of(InputSequencer.Step.clickSwap()));
        } else {
            InputSequencer.run(List.of(InputSequencer.Step.forceSneak(6)));
        }
    }

    public static KeyMapping getRepairItemMenuKey() {
        return repairItemMenuKey;
    }

    public static void blockRepairUntilRelease() {
        repairReleased = false;
    }

    public static KeyMapping getSpellLevelMenuKey() {
        return spellLevelMenuKey;
    }

    public static int compareKeyCode() {
        if (compareShopKey == null) return GLFW.GLFW_KEY_LEFT_SHIFT;
        int code = KeyBindingHelper.getBoundKeyOf(compareShopKey).getValue();
        return code == InputConstants.UNKNOWN.getValue() ? GLFW.GLFW_KEY_LEFT_SHIFT : code;
    }

    public static Component compareKeyLabel() {
        if (compareShopKey == null) return Component.translatable("key.keyboard.left.shift");
        return compareShopKey.getTranslatedKeyMessage();
    }

    public static void blockSpellLevelUntilRelease() {
        spellLevelReleased = false;
    }

    private static void handleBroadcastKey(Minecraft client) {
        if (FlashbackCompat.isInReplay()) return;
        if (client.player == null) return;
        if (!PartySync.isEnabled()) {
            client.player.displayClientMessage(
                    Component.literal("\u00a7c" + ModLanguageManager.getString(HUDConfig.get().partySyncEnabled
                            ? "keybind.broadcast.no_api" : "party_sync.disabled")), true);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastUsedTime < COOLDOWN_MS) {
            long secondsLeft = (COOLDOWN_MS - (now - lastUsedTime)) / 1000;
            String msg = ModLanguageManager.format("keybind.broadcast.cooldown", "s", secondsLeft);
            client.player.displayClientMessage(Component.literal("\u00a7c" + msg), true);
            return;
        }
        lastUsedTime = now;
        PartySync.shareHeldItem(client);
    }

    private static void handlePingKey(Minecraft client) {
        if (FlashbackCompat.isInReplay()) return;
        if (client.player == null) return;
        if (!PartySync.isEnabled()) {
            client.player.displayClientMessage(
                    Component.literal("\u00a7c" + ModLanguageManager.getString(HUDConfig.get().partySyncEnabled
                            ? "keybind.ping.no_api" : "party_sync.disabled")), true);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPingTime < PING_COOLDOWN_MS) {
            return;
        }
        lastPingTime = now;
        PartySync.pingLookedAt(client);
    }
}
