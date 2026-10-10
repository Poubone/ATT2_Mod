package fr.poubone.att2.client.gambling;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.shop.*;
import fr.poubone.att2.client.gambling.GamblingModel.BetEntry;
import fr.poubone.att2.client.gambling.GamblingModel.CountEntry;
import fr.poubone.att2.client.gambling.GamblingModel.GridCell;
import fr.poubone.att2.client.gambling.GamblingModel.GridChoice;
import fr.poubone.att2.client.gambling.widget.ListEntryButton;
import fr.poubone.att2.client.gambling.widget.TabButton;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestBookTextures;
import fr.poubone.att2.client.quest.widget.IconButton;
import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import fr.poubone.att2.client.util.LoadingText;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "Charles' counter": the bets and the Treasure Grids of the gambling NPC, rendered in the quest book style.
 * Every action sends exactly the trigger the corresponding chat link would send.
 */
public class CharlesScreen extends ShopPanelScreen {
    public enum Tab { BETS, GRIDS }

    private static final SoundEvent BOOK_OPEN = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private static final int BG_WIDTH = 760;
    private static final int BG_HEIGHT = 464;
    private static final int ROW_HEIGHT = 30;
    private static final int RIGHT_X = 198;
    private static final int RIGHT_WIDTH = 510;
    private static final int GRID_FOOTER = 56;

    private static Tab lastTab = Tab.BETS;

    private int offsetX;
    private int offsetY;
    private Tab tab;
    private final List<QuestBookWidget> tabWidgets = new ArrayList<>();
    private QuestBookWidget hovered;
    private int gridLeft;
    private int gridTop;
    private int cellSize;
    private int lastRenderedState = -1;
    private int page;
    private int pages = 1;
    private ShopActionButton previousPage, nextPage, resetBoard, backToList;

    private CharlesScreen(Tab tab) {
        super(ModLanguageManager.get("charles.title"), BG_WIDTH, BG_HEIGHT);
        this.tab = tab;
    }

    public static void open(Tab tab) {
        if (FlashbackCompat.isInReplay()) return;
        if (!fr.poubone.att2.client.hud.HUDConfig.get().charlesAutoOpen) return;
        Minecraft client = Minecraft.getInstance();
        boolean alreadyOpen = client.screen instanceof CharlesScreen;
        if (alreadyOpen) {
            ((CharlesScreen) client.screen).switchTab(tab, false);
            return;
        }
        client.setScreen(new CharlesScreen(tab));
        client.getSoundManager().play(SimpleSoundInstance.forUI(BOOK_OPEN, 1.0F));
    }

    public static void toggle() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof CharlesScreen) {
            client.setScreen(null);
        } else if (client.screen == null) {
            open(lastTab);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        initViewport();
        offsetX = 0; offsetY = 0;
        chrome(714, 25, 22, 22, ModLanguageManager.get("ui.close"), ShopActionButton.Chrome.CLOSE, ShopType.CHARLES, this::onClose);
        chrome(520, 70, 80, 20, ModLanguageManager.get("charles.reload"), ShopActionButton.Chrome.ACTION, ShopType.CHARLES, this::requestCurrentTab);
        for (Tab value : Tab.values()) {
            addRenderableWidget(new ShopActionButton(24, 92 + value.ordinal() * 34, 132, 27,
                    new ShopAction(-1, ModLanguageManager.get(value == Tab.BETS ? "charles.tab.bets" : "charles.tab.grids"), null, ShopAction.Kind.OTHER),
                    ShopType.CHARLES.titleColor, 0xFFFFE6BB, () -> switchTab(value, true),
                    ShopActionButton.Chrome.TAB, () -> tab == value, ShopType.CHARLES));
        }
        switchTab(tab, false);
        if (needsData()) requestCurrentTab();
    }

    private boolean needsData() {
        GamblingModel model = GamblingModel.get();
        return tab == Tab.BETS ? model.getBets().isEmpty() : (!model.hasGrid() && model.getGridChoices().isEmpty());
    }

    private void requestCurrentTab() {
        GamblingModel model = GamblingModel.get();
        if (tab == Tab.BETS) {
            model.requestBets();
        } else {
            model.requestGridMenu();
        }
    }

    private void switchTab(Tab newTab, boolean requestIfEmpty) {
        if (tab != newTab) page = 0;
        tab = newTab;
        lastTab = newTab;
        GamblingModel.get().clearNotEnoughChronotons();
        rebuildTabWidgets();
        if (requestIfEmpty && needsData()) requestCurrentTab();
    }

    // ---------------------------------------------------------------- right page widgets

    private void rebuildTabWidgets() {
        for (QuestBookWidget widget : tabWidgets) removeWidget(widget);
        tabWidgets.clear();
        if (previousPage != null) removeWidget(previousPage);
        if (nextPage != null) removeWidget(nextPage);
        if (resetBoard != null) { removeWidget(resetBoard); resetBoard = null; }
        if (backToList != null) { removeWidget(backToList); backToList = null; }
        GamblingModel model = GamblingModel.get();
        int x = offsetX + RIGHT_X;
        int y = offsetY + 104;

        if (tab == Tab.BETS) {
            buildPageNavigation(model.getBets().size());
            for (BetEntry bet : model.getBets().subList(page * 8, Math.min(model.getBets().size(), page * 8 + 8))) {
                List<Component> tooltip = new ArrayList<>();
                if (bet.locked()) {
                    tooltip.add(Component.translatable("att2.lock"));
                } else {
                    tooltip.add(bet.label().copy().withStyle(ChatFormatting.BOLD));
                    tooltip.add(Component.literal(ModLanguageManager.format("charles.price", "price", bet.price())).withStyle(ChatFormatting.YELLOW));
                    tooltip.add(ModLanguageManager.get("charles.bet.click").withStyle(ChatFormatting.GREEN));
                }
                Component value = bet.locked() ? null : Component.literal(bet.price() + " \u00a76\u25cf");
                addTabWidget(new ListEntryButton(x, y, RIGHT_WIDTH, ROW_HEIGHT - 2, bet.label(), value, tooltip,
                        () -> !bet.locked() && !model.isBetPending(), () -> model.placeBet(bet)));
                y += ROW_HEIGHT;
            }
            return;
        }

        if (model.hasGrid()) {
            pages = 1; page = 0; layoutGrid();
            int buttonsY = BG_HEIGHT - GRID_FOOTER;
            int half = (RIGHT_WIDTH - 12) / 2;
            resetBoard = chrome(RIGHT_X, buttonsY, half, 28,
                    Component.translatable("matching_game.reset"), ShopActionButton.Chrome.ACTION, ShopType.CHARLES,
                    model::resetGrid);
            backToList = chrome(RIGHT_X + half + 12, buttonsY, half, 28,
                    Component.translatable("matching_game.back_menu"), ShopActionButton.Chrome.ACTION, ShopType.CHARLES,
                    model::requestGridMenu);
            return;
        }

        buildPageNavigation(model.getGridChoices().size());
        for (GridChoice choice : model.getGridChoices().subList(page * 8, Math.min(model.getGridChoices().size(), page * 8 + 8))) {
            List<Component> tooltip = new ArrayList<>();
            if (choice.locked()) {
                tooltip.add(Component.translatable("matching_game.select.lock.hover_event"));
            } else {
                tooltip.add(choice.label().copy().withStyle(ChatFormatting.BOLD));
                if (choice.price() >= 0) {
                    tooltip.add(Component.literal(ModLanguageManager.format("charles.price", "price", choice.price())).withStyle(ChatFormatting.YELLOW));
                }
                tooltip.add(ModLanguageManager.get(choice.isContinue() ? "charles.grid.continue.tip" : "charles.grid.buy.tip").withStyle(ChatFormatting.GREEN));
            }
            Component value = choice.locked() || choice.price() < 0 ? null : Component.literal(choice.price() + " \u00a76\u25cf");
            addTabWidget(new ListEntryButton(x, y, RIGHT_WIDTH, ROW_HEIGHT - 2, choice.label(), value, tooltip,
                    () -> !choice.locked(), () -> model.chooseGrid(choice)));
            y += ROW_HEIGHT;
        }
    }

    private void buildPageNavigation(int count) {
        pages = Math.max(1, (count + 7) / 8);
        page = Math.max(0, Math.min(page, pages - 1));
        previousPage = chrome(618, 70, 20, 20, Component.literal("‹"), ShopActionButton.Chrome.NAV, ShopType.CHARLES,
                () -> { page--; rebuildTabWidgets(); });
        nextPage = chrome(690, 70, 20, 20, Component.literal("›"), ShopActionButton.Chrome.NAV, ShopType.CHARLES,
                () -> { page++; rebuildTabWidgets(); });
        previousPage.active = page > 0;
        nextPage.active = page + 1 < pages;
    }

    private void addTabWidget(QuestBookWidget widget) {
        tabWidgets.add(widget);
        addRenderableWidget(widget);
    }

    private void layoutGrid() {
        List<List<GridCell>> grid = GamblingModel.get().getGrid();
        int rows = Math.max(1, grid.size());
        int cols = grid.stream().mapToInt(List::size).max().orElse(1);
        int availableWidth = RIGHT_WIDTH;
        int counterRows = Math.max(1, (GamblingModel.get().getCounts().size() + 13) / 14);
        int availableHeight = BG_HEIGHT - GRID_FOOTER - 104 - counterRows * 14;
        cellSize = Math.max(1, Math.min(42, Math.min(availableWidth / cols, availableHeight / rows)));
        gridLeft = offsetX + RIGHT_X + (availableWidth - cols * cellSize) / 2;
        gridTop = offsetY + 104;
    }

    /** Rebuilds the right page whenever the model changed (new lines from the map). */
    @Override
    public void tick() {
        super.tick();
        GamblingModel model = GamblingModel.get();
        int state = model.getBets().size() * 31 + model.getGridChoices().size() * 7 + (model.hasGrid() ? 1 : 0)
                + model.getGrid().stream().flatMap(List::stream).mapToInt(c -> c.type().ordinal() + 1).sum() * 131
                + (model.isBetPending() ? 1000003 : 0);
        if (state != lastRenderedState) {
            lastRenderedState = state;
            rebuildTabWidgets();
        }
    }

    // ---------------------------------------------------------------- rendering

    @Override
    protected void renderPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        panel(graphics, ShopType.CHARLES);
        ShopTheme.text(graphics, ModLanguageManager.get("charles.title"), 62, 27, 410, 1.7f, 0xFFF0E3CC, false);
        fr.poubone.att2.client.shop.ShopSkin.texture(graphics, ShopType.CHARLES, "wallet", 574, 19, 124, 37, 232, 70);
        var balanceScore = ScoreCache.get("CHRONOTON");
        String balance = LoadingText.of(balanceScore);
        ShopTheme.text(graphics, Component.literal(ModLanguageManager.format("charles.balance", "n", balance)), 584, 32, 104, 0.85f, 0xFFE8C86A, false);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics, mouseX, mouseY, partialTick);

        hovered = null;
        for (GuiEventListener child : this.children()) {
            if (child instanceof QuestBookWidget widget && widget.isMouseOver(mouseX, mouseY)) hovered = widget;
        }

        for (var child : children()) {
            if (child instanceof ShopActionButton action
                    && ShopHover.showsTooltip(action.isHovered(), action.isFocused())) {
                showTooltip(graphics, action.tooltipLines(), mouseX, mouseY);
            }
        }
        if (!GamblingModel.get().hasGrid() || tab == Tab.BETS)
            ShopTheme.text(graphics, Component.literal((page + 1) + " / " + pages), 644, 74, 40, 1f, 0xFF382B21, true);
        renderLeftPage(graphics);
        if (tab == Tab.BETS) {
            renderBetsPage(graphics);
        } else {
            renderGridsPage(graphics, mouseX, mouseY);
        }

        if (hovered != null && !hovered.getTooltipLines().isEmpty()) {
            showTooltip(graphics, hovered.getTooltipLines(), mouseX, mouseY);
        }
    }

    private void renderLeftPage(GuiGraphics graphics) {
        HUDConfig config = HUDConfig.get();
        int textColor = 0xFF382B21;
        int secondary = 0xFF705E47;
        GamblingModel model = GamblingModel.get();
        int x = offsetX + 24;
        int width = 132;
        int y = offsetY + 170;
        int bottom = offsetY + 400;

        var balanceScore = ScoreCache.get("CHRONOTON");
        String balance = LoadingText.of(balanceScore);
        y = drawWrapped(graphics, Component.literal(ModLanguageManager.format("charles.balance", "n", balance)), x, y, width, textColor, bottom) + 3;

        if (model.isNotEnoughChronotons()) {
            y = drawWrapped(graphics, ModLanguageManager.get("charles.not_enough").withStyle(ChatFormatting.RED), x, y, width, textColor, bottom) + 2;
        }
        if (tab == Tab.GRIDS && !model.isNearTable()) {
            String distance = String.valueOf((int) model.distanceToTable());
            y = drawWrapped(graphics, Component.literal(ModLanguageManager.format("charles.too_far", "d", distance)).withStyle(ChatFormatting.RED), x, y, width, textColor, bottom) + 2;
        }
        if (model.isWaiting()) {
            y = drawWrapped(graphics, ModLanguageManager.get("quest_book.loading"), x, y, width, secondary, bottom) + 2;
        }
        if (tab == Tab.BETS) {
            if (model.isBetPending()) {
                y = drawWrapped(graphics, ModLanguageManager.get("charles.bet.pending"), x, y, width, secondary, bottom) + 2;
            }
            if (model.getLastBetScore() >= 0 && System.currentTimeMillis() - model.getLastBetScoreTime() < 120_000) {
                y = drawWrapped(graphics, Component.translatable("att2.gambling.score", model.getLastBetScore()), x, y, width, textColor, bottom) + 2;
            }
            drawWrapped(graphics, ModLanguageManager.get("charles.bets.help"), x, y, width, secondary, bottom);
        } else {
            if (model.hasGrid()) {
                if (model.getNextFlipPrice() >= 0) {
                    ChatFormatting color = model.isNextFlipAffordable() ? ChatFormatting.GREEN : ChatFormatting.RED;
                    y = drawWrapped(graphics, Component.literal(ModLanguageManager.format("charles.grid.next_price", "price", model.getNextFlipPrice())).withStyle(color), x, y, width, textColor, bottom) + 2;
                }
                if (model.isGridComplete()) {
                    y = drawWrapped(graphics, ModLanguageManager.get("charles.grid.complete").withStyle(ChatFormatting.GREEN), x, y, width, textColor, bottom) + 2;
                }
            }
            drawWrapped(graphics, ModLanguageManager.get("charles.grids.help"), x, y, width, secondary, bottom);
        }
    }

    private void renderBetsPage(GuiGraphics graphics) {
        GamblingModel model = GamblingModel.get();
        if (model.getBets().isEmpty()) {
            Component hint = model.isWaiting() ? ModLanguageManager.get("quest_book.loading") : ModLanguageManager.get("charles.bets.empty");
            drawWrapped(graphics, hint, offsetX + RIGHT_X, offsetY + 90, RIGHT_WIDTH, HUDConfig.color(HUDConfig.get().questBookSecondaryTextColor, 0xFF404040), offsetY + BG_HEIGHT);
        }
    }

    private void renderGridsPage(GuiGraphics graphics, int mouseX, int mouseY) {
        GamblingModel model = GamblingModel.get();
        int secondary = 0xFF705E47;

        if (!model.hasGrid()) {
            if (model.getGridChoices().isEmpty()) {
                Component hint = model.isWaiting() ? ModLanguageManager.get("quest_book.loading") : ModLanguageManager.get("charles.grids.empty");
                drawWrapped(graphics, hint, offsetX + RIGHT_X, offsetY + 90, RIGHT_WIDTH, secondary, offsetY + BG_HEIGHT);
            }
            return;
        }

        List<List<GridCell>> grid = model.getGrid();
        GridCell hoveredCell = null;
        for (int row = 0; row < grid.size(); row++) {
            List<GridCell> cells = grid.get(row);
            for (int col = 0; col < cells.size(); col++) {
                GridCell cell = cells.get(col);
                int x = gridLeft + col * cellSize;
                int y = gridTop + row * cellSize;
                boolean over = mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize;
                // Card slot: a darker square with the symbol centred and some breathing room around it
                fr.poubone.att2.client.shop.ShopSkin.texture(graphics, ShopType.CHARLES, over && cell.isLocked() ? "card_hover" : "card_normal", x + 1, y + 1, Math.max(1, cellSize - 2), Math.max(1, cellSize - 2), 312, 252);
                if (over && cell.isLocked()) hoveredCell = cell;
                int inset = Math.max(0, cellSize / 6);
                int iconSize = cellSize - 2 * inset;
                int textureSize = cell.type().textureSize;
                graphics.blit(RenderPipelines.GUI_TEXTURED, cell.type().texture, x + inset, y + inset, 0f, 0f,
                        iconSize, iconSize, textureSize, textureSize, textureSize, textureSize);
            }
        }

        // Counters, like the map's strip: icon then count (yellow = even, green = one more card needed)
        int y = gridTop + grid.size() * cellSize + 4;
        int x = offsetX + RIGHT_X;
        for (CountEntry entry : model.getCounts()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, entry.type().texture, x, y - 1, 0f, 0f, 10, 10,
                    entry.type().textureSize, entry.type().textureSize,
                    entry.type().textureSize, entry.type().textureSize);
            String count = String.valueOf(entry.count());
            int color = entry.color() == null ? 0xFF382B21 : (0xFF000000 | entry.color().getValue());
            graphics.drawString(this.font, count, x + 11, y, color, false);
            x += 36;
            if (x > offsetX + RIGHT_X + RIGHT_WIDTH - 20) {
                x = offsetX + RIGHT_X;
                y += 14;
            }
        }

        if (hoveredCell != null && hovered == null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("matching_game.hover_event"));
            if (model.getNextFlipPrice() >= 0) {
                lines.add(Component.literal(ModLanguageManager.format("charles.price", "price", model.getNextFlipPrice()))
                        .withStyle(model.isNextFlipAffordable() ? ChatFormatting.YELLOW : ChatFormatting.RED));
            }
            if (!model.isNearTable()) {
                lines.add(ModLanguageManager.get("charles.too_far.short").withStyle(ChatFormatting.RED));
            }
            showTooltip(graphics, lines, mouseX, mouseY);
        }
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int defaultColor, int bottom) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (FormattedCharSequence line : lines) {
            if (y + this.font.lineHeight > bottom) break;
            graphics.drawString(this.font, line, x, y, defaultColor, false);
            y += this.font.lineHeight;
        }
        return y;
    }

    // ---------------------------------------------------------------- input

    @Override
    protected boolean clickContent(MouseButtonEvent event, boolean doubleClick) {
        if (super.clickContent(event, doubleClick)) return true;
        if (tab != Tab.GRIDS || event.button() != 0) return false;
        if (event.y() >= BG_HEIGHT - GRID_FOOTER) return false;

        GamblingModel model = GamblingModel.get();
        List<List<GridCell>> grid = model.getGrid();
        for (int row = 0; row < grid.size(); row++) {
            List<GridCell> cells = grid.get(row);
            for (int col = 0; col < cells.size(); col++) {
                int x = gridLeft + col * cellSize;
                int y = gridTop + row * cellSize;
                if (event.x() >= x && event.x() < x + cellSize && event.y() >= y && event.y() < y + cellSize) {
                    GridCell cell = cells.get(col);
                    if (cell.isLocked()) {
                        model.flip(cell);
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (GamblingModel.get().hasGrid()) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        int next = ShopPaging.afterScroll(page, pages, deltaY);
        if (next != page) {
            page = next;
            rebuildTabWidgets();
            return true;
        }
        if (deltaY != 0) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
