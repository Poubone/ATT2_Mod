package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.discord.DiscordPresence;
import fr.poubone.att2.client.sync.PartySync;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Categorized settings: HUD widgets, menus, loot beams, performance, then general / Discord.
 */
public class HUDConfigScreen extends Screen {
    private enum Category {HUD, MENUS, QUESTS, LOOT, PERFORMANCE, GENERAL}

    private static final List<String> RARITY_KEYS = List.of(
            "com", "cur", "epi", "epi_set", "leg", "leg_armset",
            "misc", "myt", "que", "rar", "spe", "ult", "unc", "unk", "epi_esc", "esc");

    private Category category = Category.HUD;
    private int langIndex;

    public HUDConfigScreen() {
        super(ModLanguageManager.get("screen.hud_config.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        HUDConfig config = HUDConfig.get();
        langIndex = Math.max(0, ModLanguageManager.CODES.indexOf(config.modLanguage));
        boolean rtl = ModLanguageManager.isRightToLeft();

        int tabW = 112;
        int tabX = rtl ? width - tabW - 12 : 12;
        int tabY = HUDConfigLayout.tabTop(height, Category.values().length);
        for (Category cat : Category.values()) {
            Category picked = cat;
            Component label = ModLanguageManager.get("screen.hud_config.cat." + cat.name().toLowerCase());
            if (cat == category) {
                label = Component.literal("▸ ").append(label);
            }
            addRenderableWidget(Button.builder(label, b -> {
                category = picked;
                init();
            }).bounds(tabX, tabY, tabW, 20).build());
            tabY += HUDConfigLayout.tabStep(height, Category.values().length);
        }

        int contentX = rtl ? 16 : 140;
        int contentY = 40;
        int contentRight = rtl ? width - 140 : width;
        int colW = Math.min(360, Math.max(220, contentRight - contentX - 24));

        switch (category) {
            case HUD -> {
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_chronoton",
                        config.showChronoton, v -> config.showChronoton = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_xp",
                        config.showXP, v -> config.showXP = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_mana",
                        config.showMana, v -> config.showMana = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_stats",
                        config.showStats, v -> config.showStats = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_arrows",
                        config.showArrows, v -> config.showArrows = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_armor_durability",
                        config.showArmorDurability, v -> config.showArmorDurability = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_spell_bar",
                        config.showSpellBar, v -> config.showSpellBar = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.show_temperature",
                        config.showTemperature, v -> config.showTemperature = v);
                addRenderableWidget(Button.builder(ModLanguageManager.get("screen.hud_config.arrange"),
                        b -> Minecraft.getInstance().setScreen(new HudLayoutScreen(this)))
                        .bounds(contentX, contentY + 8, 200, 20).build());
            }
            case QUESTS -> {
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.quest_show_main",
                        config.questBookShowMain, v -> config.questBookShowMain = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.quest_show_side",
                        config.questBookShowSide, v -> config.questBookShowSide = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.quest_show_completed",
                        config.questBookShowCompleted, v -> config.questBookShowCompleted = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.quest_show_daily",
                        config.questBookShowDaily, v -> config.questBookShowDaily = v);
            }
            case MENUS -> {
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.quest_menu",
                        config.questMenuEnabled, v -> config.questMenuEnabled = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.rune_menu",
                        config.runeMenuEnabled, v -> config.runeMenuEnabled = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.miner_menu",
                        config.minerMenuEnabled, v -> config.minerMenuEnabled = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.charles_auto_open",
                        config.charlesAutoOpen, v -> config.charlesAutoOpen = v);
                int shopX = contentX;
                int shopY = contentY;
                int shopIndex = 0;
                for (String shopId : HUDConfig.SHOP_MENU_IDS) {
                    String id = shopId;
                    addToggle(shopX, shopY, 170, "shop.type." + id,
                            config.isShopMenuEnabled(id), selected -> config.setShopMenuEnabled(id, selected));
                    shopIndex++;
                    if (shopIndex % 2 == 0) {
                        shopX = contentX;
                        shopY += 22;
                    } else {
                        shopX += 180;
                    }
                }
            }
            case LOOT -> {
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.render_allItems",
                        config.allItems, v -> config.allItems = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.render_nametags",
                        config.renderNametags, v -> config.renderNametags = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.render_stackcount",
                        config.renderStackcount, v -> config.renderStackcount = v);
                contentY += 6;
                int rx = contentX;
                int ry = contentY;
                int i = 0;
                for (String rarity : RARITY_KEYS) {
                    boolean on = config.renderRarities.contains(rarity);
                    addToggle(rx, ry, 150, "screen.hud_config.render_rarity." + rarity, on, selected -> {
                        if (selected) {
                            if (!config.renderRarities.contains(rarity)) config.renderRarities.add(rarity);
                        } else {
                            config.renderRarities.remove(rarity);
                        }
                    });
                    i++;
                    if (i % 3 == 0) {
                        rx = contentX;
                        ry += 22;
                    } else {
                        rx += 160;
                    }
                }
            }
            case PERFORMANCE -> {
                int performanceWidth = Math.max(1, Math.min(260, contentRight - contentX - 16));
                contentY = addToggle(contentX, contentY, performanceWidth, "screen.hud_config.orb_gpu",
                        config.orbGpuRendering, v -> {
                            config.orbGpuRendering = v;
                            init(); // an automatic orb rate follows the switch, so refresh the slider
                        }, "screen.hud_config.orb_gpu.tooltip");
                contentY += 4;
                addOrbFpsSlider(contentX, contentY, performanceWidth, config);
            }
            case GENERAL -> {
                addRenderableWidget(Button.builder(
                        ModLanguageManager.get("screen.hud_config.language_button").copy()
                                .append(Component.literal(" : " + ModLanguageManager.nativeName(config.modLanguage))),
                        b -> cycleLanguage()
                ).bounds(contentX, contentY, 200, 20).build());
                contentY += 28;
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.drop_lock",
                        config.dropLockEnabled, v -> config.dropLockEnabled = v);
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.party_sync",
                        config.partySyncEnabled, v -> {
                            config.partySyncEnabled = v;
                            PartySync.onConfigChanged();
                        });
                contentY = addToggle(contentX, contentY, colW, "screen.hud_config.discord_rpc",
                        config.discordRichPresence, v -> {
                            config.discordRichPresence = v;
                            DiscordPresence.onConfigChanged();
                        });
                addToggle(contentX, contentY, colW, "screen.hud_config.discord_show_quest",
                        config.discordShowQuest, v -> {
                            config.discordShowQuest = v;
                            DiscordPresence.onConfigChanged();
                        });
            }
        }

        addRenderableWidget(Button.builder(ModLanguageManager.get("screen.hud_config.save_and_close"), b -> {
            HUDConfig.save();
            DiscordPresence.onConfigChanged();
            Minecraft.getInstance().setScreen(null);
        }).bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private int addToggle(int x, int y, int width, String key, boolean selected, Consumer<Boolean> onChange) {
        return addToggle(x, y, width, key, selected, onChange, null);
    }

    private int addToggle(int x, int y, int width, String key, boolean selected, Consumer<Boolean> onChange, String tooltipKey) {
        Checkbox.Builder builder = Checkbox.builder(ModLanguageManager.get(key), font)
                .pos(x, y)
                .selected(selected)
                .onValueChange((checkbox, value) -> onChange.accept(value));
        if (tooltipKey != null) {
            builder.maxWidth(width);
            builder.tooltip(Tooltip.create(ModLanguageManager.get(tooltipKey)));
        }
        Checkbox box = builder.build();
        addRenderableWidget(box);
        // Eight HUD toggles must leave room for Arrange and Save at the minimum GUI height.
        int spacing = category == Category.HUD && height < 280 ? 18 : 22;
        return y + (tooltipKey == null ? spacing : Math.max(box.getHeight() + 2, spacing));
    }

    /** Orb redraw rate, snapping to {@link HUDConfig#ORB_FPS_STEPS}; the last step redraws every frame. */
    private void addOrbFpsSlider(int x, int y, int width, HUDConfig config) {
        int[] steps = HUDConfig.ORB_FPS_STEPS;
        // Opens on the rate in use; the automatic default only becomes a stored choice once moved
        int current = config.effectiveOrbFps();
        int index = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == current) index = i;
        }
        AbstractSliderButton slider = new AbstractSliderButton(x, y, width, 20, Component.empty(),
                index / (double) (steps.length - 1)) {
            {
                updateMessage();
            }

            private int fps() {
                return steps[(int) Math.round(this.value * (steps.length - 1))];
            }

            @Override
            protected void updateMessage() {
                int fps = fps();
                setMessage(Component.literal(fps == 0
                        ? ModLanguageManager.getString("screen.hud_config.orb_fps.every_frame")
                        : ModLanguageManager.format("screen.hud_config.orb_fps", "fps", fps)));
            }

            @Override
            protected void applyValue() {
                config.orbFps = fps();
            }
        };
        slider.setTooltip(Tooltip.create(ModLanguageManager.get("screen.hud_config.orb_fps.tooltip")));
        addRenderableWidget(slider);
    }

    private void cycleLanguage() {
        langIndex = (langIndex + 1) % ModLanguageManager.CODES.size();
        String newLang = ModLanguageManager.CODES.get(langIndex);
        HUDConfig.setModLanguage(newLang);
        ModLanguageManager.loadLanguage(Minecraft.getInstance(), newLang);
        init();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        boolean rtl = ModLanguageManager.isRightToLeft();
        if (rtl) {
            context.fill(width - 128, 0, width, height, 0xCC14110E);
            context.fill(0, 0, width - 128, height, 0x99110E0C);
        } else {
            context.fill(0, 0, 128, height, 0xCC14110E);
            context.fill(128, 0, width, height, 0x99110E0C);
        }
        String title = ModLanguageManager.getString("screen.hud_config.title");
        String cat = ModLanguageManager.getString("screen.hud_config.cat." + category.name().toLowerCase());
        if (rtl) {
            context.drawString(font, title, width - 12 - font.width(title), 12, 0xFFE8C86A, false);
            context.drawString(font, cat, 16, 14, 0xFFC8B8A0, false);
            int markY = HUDConfigLayout.tabTop(height, Category.values().length)
                    + category.ordinal() * HUDConfigLayout.tabStep(height, Category.values().length);
            context.fill(width - 11, markY, width - 8, markY + 20, 0xFFE8C86A);
        } else {
            context.drawString(font, title, 12, 12, 0xFFE8C86A, false);
            context.drawString(font, cat, 140, 14, 0xFFC8B8A0, false);
            int markY = HUDConfigLayout.tabTop(height, Category.values().length)
                    + category.ordinal() * HUDConfigLayout.tabStep(height, Category.values().length);
            context.fill(8, markY, 11, markY + 20, 0xFFE8C86A);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
