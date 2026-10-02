package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.data.AttributeDialogParser;
import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.HeldWeaponStats;
import fr.poubone.att2.client.data.StatCaps;
import fr.poubone.att2.client.data.StatEffectText;
import fr.poubone.att2.client.data.StatManager;
import fr.poubone.att2.client.data.StatUpgradeModel;
import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.util.ModLanguageManager;
import fr.poubone.att2.client.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class StatUpgradeScreen extends Screen {

    private static final int OUTER_MARGIN = 24;
    private static final int PANEL_MAX_W = 640;
    private static final int PANEL_MAX_H = 400;
    private static final int PAD = 12;
    private static final int HEADER_H = 14;
    private static final int COLS = 3;
    /** Wide enough for FR labels like « Vitesse de déplacement ». */
    private static final int CARD_W = 188;
    private static final int CARD_H = 76;
    private static final int MIN_CARD_H = 48;
    private static final int GAP = 10;
    private static final int ICON_SIZE = 16;
    private static final int PLUS_SIZE = 20;

    private record CardHit(String key, int x, int y, int w, int h, Button plus) {}

    private final List<CardHit> cards = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int cardH = CARD_H;
    private int gap = GAP;
    private int layoutPad = PAD;
    private int layoutHeaderGap = 6;
    private int cachedSkillPoints;
    private Button closeButton;

    public StatUpgradeScreen() {
        super(ModLanguageManager.get("screen.stat_upgrade.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        cards.clear();
        StatManager.updateAll();
        if (!StatUpgradeModel.isListening()) {
            StatUpgradeModel.open();
        }

        layoutPanel();
        int i = 0;
        for (String key : StatManager.STAT_KEYS) {
            Button plus = new KenneyPlusButton(0, 0, PLUS_SIZE, PLUS_SIZE, btn -> {
                Integer trigger = Att2Triggers.STAT_UPGRADE.get(key);
                if (trigger != null) {
                    Att2Triggers.send(trigger);
                }
            });
            this.addRenderableWidget(plus);
            cards.add(new CardHit(key, 0, 0, CARD_W, cardH, plus));
            i++;
        }

        closeButton = Button.builder(ModLanguageManager.get("screen.stat_upgrade.close"), btn -> {
            Minecraft.getInstance().setScreen(null);
        }).bounds(0, 0, 80, PLUS_SIZE).build();
        this.addRenderableWidget(closeButton);
        syncCardWidgets();
    }

    private void layoutPanel() {
        panelW = Math.min(this.width - OUTER_MARGIN, PANEL_MAX_W);
        int availH = this.height - OUTER_MARGIN;
        int maxPreferred = Math.min(availH, PANEL_MAX_H);

        cardH = CARD_H;
        gap = GAP;
        layoutPad = PAD;
        layoutHeaderGap = 6;

        int needed = contentHeight();
        panelH = Math.min(availH, Math.max(needed, maxPreferred));

        while (panelH < contentHeight() && gap > 4) {
            gap--;
            needed = contentHeight();
        }
        while (panelH < contentHeight() && layoutHeaderGap > 2) {
            layoutHeaderGap--;
            needed = contentHeight();
        }
        while (panelH < contentHeight() && layoutPad > 6) {
            layoutPad--;
            needed = contentHeight();
        }

        if (panelH < contentHeight()) {
            int rows = (StatManager.STAT_KEYS.size() + COLS - 1) / COLS;
            int fixed = layoutPad + HEADER_H + layoutHeaderGap + layoutPad + PLUS_SIZE + (rows - 1) * gap;
            int forCards = panelH - fixed;
            if (rows > 0 && forCards > 0) {
                cardH = Math.max(MIN_CARD_H, (forCards - (rows - 1) * gap) / rows);
            }
        }

        needed = contentHeight();
        if (needed <= availH) {
            panelH = Math.max(needed, Math.min(panelH, maxPreferred));
            panelH = Math.min(availH, panelH);
        }

        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
    }

    private int contentHeight() {
        int rows = (StatManager.STAT_KEYS.size() + COLS - 1) / COLS;
        return layoutPad + HEADER_H + layoutHeaderGap + rows * cardH + (rows - 1) * gap + layoutPad + PLUS_SIZE;
    }

    private void syncCardWidgets() {
        if (cards.isEmpty()) {
            return;
        }
        int gridW = COLS * CARD_W + (COLS - 1) * gap;
        int gridX = panelX + (panelW - gridW) / 2;
        int gridY = panelY + layoutPad + HEADER_H + layoutHeaderGap;
        int plusMargin = scaleCard(cardH, 4);

        for (int i = 0; i < cards.size(); i++) {
            CardHit card = cards.get(i);
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridX + col * (CARD_W + gap);
            int cy = gridY + row * (cardH + gap);
            cards.set(i, new CardHit(card.key, cx, cy, CARD_W, cardH, card.plus));
            card.plus.setPosition(cx + CARD_W - PLUS_SIZE - plusMargin, cy + cardH - PLUS_SIZE - plusMargin);
        }

        if (closeButton != null) {
            int closeY = panelY + panelH - layoutPad - PLUS_SIZE;
            closeButton.setPosition(panelX + (panelW - 80) / 2, closeY);
        }
    }

    private static int scaleCard(int cardHeight, int nominalAt72) {
        return StatUpgradeCardLayout.scaleCard(cardHeight, nominalAt72);
    }

    @Override
    public void removed() {
        StatUpgradeModel.close();
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        drawPanelChrome(context, cachedSkillPoints);
        for (CardHit card : cards) {
            drawCard(context, card, cachedSkillPoints);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        StatManager.updateAll();
        layoutPanel();
        syncCardWidgets();
        cachedSkillPoints = StatUpgradeModel.skillPoints();
        for (CardHit card : cards) {
            updatePlusState(card, cachedSkillPoints);
        }

        super.render(context, mouseX, mouseY, delta);

        CardHit hovered = null;
        for (CardHit card : cards) {
            if (mouseX >= card.x && mouseX < card.x + card.w && mouseY >= card.y && mouseY < card.y + card.h) {
                hovered = card;
            }
        }

        if (hovered != null) {
            String key = hovered.key;
            int tot = StatManager.total(key);
            int base = StatUpgradeModel.base(key).orElse(tot);
            StatEffectText.WeaponStats weapon = HeldWeaponStats.of(Minecraft.getInstance().player);
            Component currentComp = wrapEffectLine(
                    "screen.stat_upgrade.effect.current", StatEffectText.current(key, tot, weapon));
            Component nextComp = StatEffectText.next(key, tot, base, weapon)
                    .map(v -> wrapEffectLine("screen.stat_upgrade.effect.next", v))
                    .orElse(ModLanguageManager.get("screen.stat_upgrade.maxed"));
            Component costComp = null;
            OptionalInt costOpt = StatUpgradeModel.upgradeCost(key);
            int max = StatCaps.maxBase(key);
            if (costOpt.isPresent() && costOpt.getAsInt() > 0 && base < max) {
                costComp = formatLang("screen.stat_upgrade.cost", costOpt.getAsInt())
                        .copy().withStyle(ChatFormatting.BOLD);
            }
            StatHoverTooltip.render(context, width, height, mouseX, mouseY,
                    currentComp, nextComp, costComp, sourceLines(key));
        }
    }

    private static List<Component> sourceLines(String key) {
        Map<String, Integer> sources = StatUpgradeModel.sources(key);
        if (sources.isEmpty()) return List.of();
        List<Component> lines = new ArrayList<>();
        for (String suffix : AttributeDialogParser.SOURCE_SUFFIXES) {
            Integer value = sources.get(suffix);
            if (value == null) continue;
            lines.add(formatLang("screen.stat_upgrade.source." + suffix, value));
        }
        return lines;
    }

    private void drawPanelChrome(GuiGraphics context, int skillPoints) {
        context.fill(panelX + 3, panelY + 4, panelX + panelW + 3, panelY + panelH + 4, 0x50000000);
        QuestBookSkin.panel(context, "cover", panelX, panelY, panelW, panelH, 100, 100, 6);
        QuestBookSkin.panel(context, "page", panelX + 3, panelY + 3, panelW - 6, panelH - 6, 100, 100, 4);
        Component header = Component.literal(
                ModLanguageManager.format("screen.stat_upgrade.skill_points", "points", skillPoints));
        ShopTheme.text(context, header, panelX, panelY + layoutPad, panelW, 1f, QuestBookSkin.INK, true);
    }

    private void drawCard(GuiGraphics context, CardHit card, int skillPoints) {
        String key = card.key;
        int tot = StatManager.total(key);
        int base = StatUpgradeModel.base(key).orElse(tot);
        int max = StatCaps.maxBase(key);
        OptionalInt costOpt = StatUpgradeModel.upgradeCost(key);

        QuestBookSkin.panel(context, "page", card.x, card.y, card.w, card.h, 100, 100, 4);

        StatUpgradeCardLayout.Interior layout = StatUpgradeCardLayout.layout(card.h, tot != base);
        int iconX = card.x + layout.pad();
        int iconY = card.y + layout.pad();
        if (key.equals("DAR")) {
            context.pose().pushMatrix();
            context.pose().translate(iconX, iconY);
            context.renderItem(Items.BOOK.getDefaultInstance(), 0, 0);
            context.pose().popMatrix();
        } else {
            Identifier icon = ModTextures.STAT_ICONS.get(key);
            if (icon != null) {
                context.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0f, 0f,
                        ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
        }

        int textX = card.x + layout.pad() + ICON_SIZE + 4;
        int innerW = card.w - (textX - card.x) - PLUS_SIZE - layout.pad() - 2;
        Component name = ModLanguageManager.get("stat.label." + key.toLowerCase());
        ShopTheme.text(context, name, textX, card.y + layout.pad(), innerW, 1f, QuestBookSkin.INK, false);

        Component costLine;
        if (costOpt.isEmpty()) {
            costLine = ModLanguageManager.get("screen.stat_upgrade.cost_pending");
        } else if (costOpt.getAsInt() == 0 || base >= max) {
            costLine = ModLanguageManager.get("screen.stat_upgrade.maxed");
        } else {
            costLine = formatLang("screen.stat_upgrade.cost", costOpt.getAsInt());
        }
        int detailWidth = card.w - PLUS_SIZE - layout.pad() - layout.plusMargin();
        ShopTheme.text(context, costLine.copy().withStyle(ChatFormatting.BOLD),
                card.x + layout.pad(), card.y + layout.levelY(), detailWidth, 1f, QuestBookSkin.INK, false);

        Component baseLine = formatLang("screen.stat_upgrade.level", base, max);
        int baseY = layout.showTot() ? layout.totY() : layout.costY();
        ShopTheme.text(context, baseLine, card.x + layout.pad(), card.y + baseY,
                detailWidth, 1f, QuestBookSkin.INK, false);

        if (layout.showTot()) {
            Component totLine = formatLang("screen.stat_upgrade.tot", tot);
            ShopTheme.text(context, totLine, card.x + layout.pad(), card.y + layout.costY(),
                    detailWidth, 1f, QuestBookSkin.MUTED, false);
        }
    }

    private void updatePlusState(CardHit card, int skillPoints) {
        int base = StatUpgradeModel.base(card.key).orElse(StatManager.total(card.key));
        int max = StatCaps.maxBase(card.key);
        OptionalInt costOpt = StatUpgradeModel.upgradeCost(card.key);
        boolean canUpgrade = StatUpgradeModel.hasData()
                && costOpt.isPresent()
                && costOpt.getAsInt() > 0
                && skillPoints >= costOpt.getAsInt()
                && base < max;
        card.plus.active = canUpgrade;
    }

    private static Component wrapEffectLine(String wrapperKey, StatEffectText.EffectView v) {
        return formatLang(wrapperKey, formatEffect(v));
    }

    private static String formatEffect(StatEffectText.EffectView v) {
        return formatLangString(v.langKey(), v.args());
    }

    private static Component formatLang(String key, Object... args) {
        return Component.literal(formatLangString(key, args));
    }

    /** Formats lang_mod strings using {0}, {1}, … placeholders. */
    private static String formatLangString(String key, Object... args) {
        String value = ModLanguageManager.getString(key);
        for (int i = 0; i < args.length; i++) {
            value = value.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return value;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class KenneyPlusButton extends Button {
        KenneyPlusButton(int x, int y, int w, int h, OnPress onPress) {
            super(x, y, w, h, Component.literal("+"), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
            QuestBookSkin.button(g, getX(), getY(), width, height, false, active && isHoveredOrFocused());
            int color = active ? QuestBookSkin.INK : QuestBookSkin.MUTED;
            var font = Minecraft.getInstance().font;
            g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                    getY() + (height - font.lineHeight) / 2, color, false);
            if (!active) {
                g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0x70EDE2C8);
            }
        }
    }
}
