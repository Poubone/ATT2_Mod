package fr.poubone.att2.client;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemNameTags;
import fr.poubone.att2.client.renderer.ItemParticleBudget;
import fr.poubone.att2.client.screen.HUDConfigScreen;
import fr.poubone.att2.client.util.ModLanguageManager;
import fr.poubone.att2.client.update.ModUpdateChecker;
import fr.poubone.att2.client.update.ModUpdateScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.item.ItemEntity;

import java.lang.reflect.Field;
import java.util.Map;
import java.net.URI;

/** Exercises the real mixins, shaders and widgets in a disposable world. */
public class ReleaseIntegrationTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("fill -8 -61 -8 8 -61 8 minecraft:stone");
            world.getServer().runCommand("tp @p 0 -60 0 0 15");
            world.getServer().runCommand("item replace entity @p hotbar.0 with minecraft:diamond_sword");
            world.getServer().runCommand("item replace entity @p hotbar.1 with minecraft:iron_sword");
            world.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:shield");
            for (int i = 0; i < 12; i++) {
                world.getServer().runCommand("summon minecraft:item " + (i % 4 - 2) + " -59 " + (3 + i / 4)
                        + " {Glowing:1b,PickupDelay:32767s,CustomName:'\"Integration item\"',Item:{id:\"minecraft:diamond_sword\",count:1,components:{\"minecraft:custom_data\":{Rarity:\"leg\"}}}}");
            }
            context.waitTicks(35);
            world.getClientWorld().waitForChunksRender();
            context.takeScreenshot(TestScreenshotOptions.of("release-loot-batched").withSize(1280, 720));
            context.runOnClient(client -> {
                ItemEntity item = null;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof ItemEntity found) { item = found; break; }
                }
                if (item == null) throw new AssertionError("Dropped items were not created");
                var tag = item.getDisplayName();
                ItemNameTags.remember(item, tag);
                ItemNameTags.width(client.font, tag);
                Object before = layouts().get(tag);
                ItemNameTags.remember(item, tag);
                if (before != layouts().get(tag)) throw new AssertionError("Cache hit discarded the name-tag layout");
                ItemNameTags.fontsChanged();
                ItemNameTags.width(client.font, tag);
                if (before != layouts().get(tag)) throw new AssertionError("Font reload lost the cached tag");
                HUDConfig.get().batchLootBeams = false;
                HUDConfig.get().solidItemEntities = true;
                HUDConfig.get().lootBeamLimit = 16;
                HUDConfig.get().mapItemParticleLimit = 8;
            });
            context.waitTicks(20);
            world.getServer().runCommand("particle minecraft:dust{color:[1.0,0.5,0.0],scale:1.0} 0 -59 3 0 0 0 0 3 force");
            context.waitTicks(5);
            context.runOnClient(client -> {
                if (ItemParticleBudget.isSpawningItemDust()) throw new AssertionError("Particle flag leaked after packet handling");
                client.setScreen(new InventoryScreen(client.player));
            });
            context.waitTicks(5);
            context.takeScreenshot(TestScreenshotOptions.of("release-inventory").withSize(1280, 720));
            context.runOnClient(client -> {
                client.setScreen(new HUDConfigScreen());
                ModLanguageManager.loadLanguage(client, "fr");
            });
            context.waitTicks(4);
            context.runOnClient(client -> {
                for (var child : client.screen.children()) {
                    if (child instanceof Button button && button.getMessage().getString().equals("Performances")) {
                        button.onPress(null);
                        return;
                    }
                }
                throw new AssertionError("Performance tab was not found");
            });
            context.waitTicks(5);
            context.takeScreenshot(TestScreenshotOptions.of("release-performance-fr").withSize(1280, 720));
            context.runOnClient(client -> {
                for (String lang : ModLanguageManager.CODES) {
                    ModLanguageManager.loadLanguage(client, lang);
                    for (String key : new String[]{"att2.ui.unequipped_weapon", "screen.hud_config.batch_loot_beams",
                            "screen.hud_config.shop_confirm", "shop.hide_owned"}) {
                        if (ModLanguageManager.getString(key).startsWith("§c?"))
                            throw new AssertionError(lang + " missing " + key);
                    }
                }
                ModLanguageManager.loadLanguage(client, "fr");
                client.setScreen(null);
                HUDConfig.get().batchLootBeams = true;
                HUDConfig.get().solidItemEntities = false;
            });
        }
        testUpdatePopup(context);
    }

    private static void testUpdatePopup(ClientGameTestContext context) {
        String ignored = context.computeOnClient(client -> HUDConfig.get().ignoredModUpdateVersion);
        boolean enabled = context.computeOnClient(client -> HUDConfig.get().checkModUpdates);
        try {
            context.runOnClient(client -> {
                HUDConfig.get().checkModUpdates = false;
                ModLanguageManager.loadLanguage(client, "fr");
                client.setScreen(new ModUpdateScreen(new TitleScreen(),
                        new ModUpdateChecker.Release("3.0.0", URI.create("https://guide-att2.com/mod/"))));
            });
            context.waitTicks(3);
            context.takeScreenshot(TestScreenshotOptions.of("release-update-popup-fr").withSize(1280, 720));
            context.runOnClient(client -> {
                clickPopup(client.screen.children(), ModLanguageManager.getString("update.later"));
                if (!(client.screen instanceof TitleScreen)) throw new AssertionError("Later did not return to the menu");
                client.setScreen(new ModUpdateScreen(client.screen,
                        new ModUpdateChecker.Release("3.0.0", URI.create("https://guide-att2.com/mod/"))));
                clickPopup(client.screen.children(), ModLanguageManager.getString("update.ignore"));
                if (!(client.screen instanceof TitleScreen) || !"3.0.0".equals(HUDConfig.get().ignoredModUpdateVersion))
                    throw new AssertionError("Ignore did not save the version and close the popup");
                for (String lang : ModLanguageManager.CODES) {
                    ModLanguageManager.loadLanguage(client, lang);
                    for (String key : new String[]{"update.title", "update.open_page", "update.later", "update.ignore",
                            "screen.hud_config.cat.updates", "screen.hud_config.check_updates"}) {
                        if (ModLanguageManager.getString(key).startsWith("§c?")) throw new AssertionError(lang + " missing " + key);
                    }
                    if (!ModLanguageManager.format("update.available", "version", "3.0.0").contains("3.0.0"))
                        throw new AssertionError(lang + " missing version placeholder");
                }
            });
        } finally {
            context.runOnClient(client -> {
                HUDConfig.get().ignoredModUpdateVersion = ignored;
                HUDConfig.get().checkModUpdates = enabled;
                HUDConfig.save();
            });
        }
    }

    private static void clickPopup(java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children,
                                   String label) {
        for (var child : children) {
            if (child instanceof Button button && button.getMessage().getString().equals(label)) {
                button.onPress(null);
                return;
            }
        }
        throw new AssertionError("Popup button not found: " + label);
    }

    private static Map<?, ?> layouts() {
        try {
            Field field = ItemNameTags.class.getDeclaredField("LAYOUTS");
            field.setAccessible(true);
            return (Map<?, ?>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
