package fr.poubone.att2.client.quest;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.widget.JournalButton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import fr.poubone.att2.client.quest.widget.QuestEntryButton;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Responsive two-page journal. Quest data and actions retain the original behaviour.
 * The skin uses Kenney CC0 assets.
 */
public class QuestBookScreen extends Screen {
    private static final SoundEvent BOOK_OPEN = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private static final SoundEvent BOOK_TURN_PAGE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.turn_page"));

    private QuestBookLayout layout;

    private int currentPage = 0;
    private int maxPage = 0;
    private final List<QuestInfo> elements = new ArrayList<>();
    private final List<QuestEntryButton> entryButtons = new ArrayList<>();
    private EditBox searchBox;
    private String searchText = "";
    private String selectedKey;
    private final List<Component> guideTooltip = new ArrayList<>();
    private QuestBookWidget hovered;
    private int leftScroll;
    private int leftContentHeight;
    private boolean draggingLeftScroll;

    private QuestBookScreen() {
        super(ModLanguageManager.get("quest_book.title"));
        this.selectedKey = QuestModel.get().getSelectedKey();
    }

    public static void open() {
        if (!HUDConfig.get().questMenuEnabled) {
            fr.poubone.att2.client.data.Att2Triggers.send(fr.poubone.att2.client.data.Att2Triggers.CONSCIOUSNESS_SIDEQUEST_LIST);
            return;
        }
        Minecraft client = Minecraft.getInstance();
        QuestModel.get().requestSideQuests();
        if (HUDConfig.get().questBookShowDaily) {
            QuestModel.get().requestDailyQuests();
        }
        client.setScreen(new QuestBookScreen());
        client.getSoundManager().play(SimpleSoundInstance.forUI(BOOK_OPEN, 1.0F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        layout = QuestBookLayout.fit(this.width, this.height);
        entryButtons.clear();
        draggingLeftScroll = false;
        searchBox = new EditBox(this.font, layout.rightX() + 7, layout.searchY() + 6,
                layout.contentWidth() - 14, 12, ModLanguageManager.get("quest_book.search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(64);
        searchBox.setTextColor(QuestBookSkin.INK);
        searchBox.setTextColorUneditable(QuestBookSkin.MUTED);
        searchBox.setHint(ModLanguageManager.get("quest_book.search").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setValue(searchText);
        searchBox.setResponder(text -> {
            searchText = text;
            currentPage = 0;
            reloadElements();
        });
        this.addRenderableWidget(searchBox);

        HUDConfig config = HUDConfig.get();
        addControl(layout.x() + layout.width() - 30, layout.y() + 9, 22, 22,
                "quest_book.close", ItemStack.EMPTY, "close", () -> true, () -> true, e -> onClose());
        addControl(layout.x() + layout.width() - 56, layout.y() + 9, 22, 22,
                "quest_book.reload", new ItemStack(Items.COMPASS), null, () -> false, () -> true, e -> {
                    QuestModel.get().requestSideQuests();
                    if (config.questBookShowDaily) QuestModel.get().requestDailyQuests();
                });
        Component guideLabel = ModLanguageManager.get("quest_book.guide");
        int guideWidth = this.font.width(guideLabel) + 16;
        refreshGuideTooltip();
        this.addRenderableWidget(new JournalButton(
                layout.x() + layout.width() - 60 - guideWidth, layout.y() + 9, guideWidth, 22,
                guideLabel, ItemStack.EMPTY, null,
                guideTooltip,
                () -> false, this::hasGuidePage, e -> openSelectedGuide()));
        addFilter(0, new ItemStack(Items.WRITTEN_BOOK), "main", QuestCategory.MAIN,
                () -> config.questBookShowMain, v -> config.questBookShowMain = v);
        addFilter(1, new ItemStack(Items.PAPER), "side", QuestCategory.SIDE,
                () -> config.questBookShowSide, v -> config.questBookShowSide = v);
        addFilter(2, new ItemStack(Items.CLOCK), "daily", QuestCategory.DAILY,
                () -> config.questBookShowDaily, v -> config.questBookShowDaily = v);
        addFilter(3, new ItemStack(Items.EMERALD), "completed", null,
                () -> config.questBookShowCompleted, v -> config.questBookShowCompleted = v);

        addControl(layout.rightX(), layout.footerY(), 24, 20, "quest_book.previous", ItemStack.EMPTY,
                "previous", () -> false, () -> currentPage > 0, e -> setCurrentPage(currentPage - 1));
        addControl(layout.rightX() + layout.contentWidth() - 24, layout.footerY(), 24, 20,
                "quest_book.next", ItemStack.EMPTY, "next", () -> false,
                () -> currentPage < maxPage, e -> setCurrentPage(currentPage + 1));
        reloadElements();
    }

    private enum QuestCategory { MAIN, SIDE, DAILY }

    private void addControl(int x, int y, int w, int h, String key, ItemStack item, String icon,
                            java.util.function.BooleanSupplier selected, java.util.function.BooleanSupplier available,
                            java.util.function.Consumer<MouseButtonEvent> press) {
        this.addRenderableWidget(new JournalButton(x, y, w, h, ModLanguageManager.get(key), item, icon,
                List.of(ModLanguageManager.get(key)), selected, available, press));
    }

    private void addFilter(int index, ItemStack item, String key, QuestCategory category,
                           java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(ModLanguageManager.get("quest_book.filter." + key));
        tooltip.add(ModLanguageManager.get("quest_book.filter.toggle").withStyle(ChatFormatting.GRAY));
        if (category != null) tooltip.add(ModLanguageManager.get("quest_book.filter.only").withStyle(ChatFormatting.GRAY));
        if (category == QuestCategory.DAILY) tooltip.add(ModLanguageManager.get("quest_book.daily.description"));
        int columns = layout.filterColumns();
        int x = layout.rightX() + (index % columns) * (layout.filterWidth() + 4);
        int y = layout.filterY() + (index / columns) * 22;
        this.addRenderableWidget(new JournalButton(x, y, layout.filterWidth(), 20,
                ModLanguageManager.get("quest_book.tab." + key), item, null, tooltip, getter, () -> true, event -> {
            HUDConfig config = HUDConfig.get();
            if (event.hasShiftDown() && category != null) {
                config.questBookShowMain = category == QuestCategory.MAIN;
                config.questBookShowSide = category == QuestCategory.SIDE;
                config.questBookShowDaily = category == QuestCategory.DAILY;
            } else {
                setter.accept(!getter.getAsBoolean());
            }
            if (config.questBookShowDaily && !QuestModel.get().hasDailyQuestData()) QuestModel.get().requestDailyQuests();
            HUDConfig.save();
            currentPage = 0;
            reloadElements();
        }));
    }

    private List<QuestInfo> collectQuests() {
        QuestModel model = QuestModel.get();
        HUDConfig config = HUDConfig.get();
        List<QuestInfo> quests = new ArrayList<>();
        if (config.questBookShowMain) {
            quests.add(model.getMainQuest());
        }
        if (config.questBookShowSide) {
            for (QuestInfo quest : model.getSideQuests()) {
                if (!config.questBookShowCompleted && quest.status() == QuestStatus.COMPLETED) continue;
                quests.add(quest);
            }
        }
        if (config.questBookShowDaily) {
            for (QuestInfo quest : model.getDailyQuests()) {
                if (!config.questBookShowCompleted && quest.status() == QuestStatus.COMPLETED) continue;
                quests.add(quest);
            }
        }
        String search = searchText.toLowerCase(Locale.ROOT).trim();
        if (!search.isEmpty()) {
            quests.removeIf(q -> !q.plainName().toLowerCase(Locale.ROOT).contains(search));
        }
        return quests;
    }

    private void reloadElements() {
        elements.clear();
        elements.addAll(collectQuests());
        maxPage = Math.max(0, (elements.size() + layout.rowsPerPage() - 1) / layout.rowsPerPage() - 1);
        currentPage = Mth.clamp(currentPage, 0, maxPage);

        for (QuestEntryButton button : entryButtons) {
            this.removeWidget(button);
        }
        entryButtons.clear();

        int start = currentPage * layout.rowsPerPage();
        for (int i = start; i < Math.min(elements.size(), start + layout.rowsPerPage()); i++) {
            int slot = i - start;
            QuestEntryButton button = new QuestEntryButton(
                    layout.rightX(),
                    layout.listTop() + slot * layout.rowHeight(),
                    layout.contentWidth(),
                    layout.rowHeight() - 3,
                    elements.get(i),
                    this);
            entryButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!collectQuests().equals(elements)) reloadElements();
    }

    private void setCurrentPage(int page) {
        int clamped = Mth.clamp(page, 0, maxPage);
        if (clamped == currentPage) return;
        currentPage = clamped;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BOOK_TURN_PAGE, 1.0F));
        reloadElements();
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    /** The quest shown on the left page has a page on guide-att2.com. */
    private boolean hasGuidePage() {
        return guidePage() != null;
    }

    private void refreshGuideTooltip() {
        guideTooltip.clear();
        guideTooltip.add(ModLanguageManager.get("quest_book.guide.open"));
        QuestInfo selected = QuestModel.get().findByKey(selectedKey);
        if (selected != null) {
            guideTooltip.add(selected.name().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /** Opens the guide in the browser, on the book for the quest shown on the left page. */
    private void openSelectedGuide() {
        URI page = guidePage();
        if (page == null) return;
        try {
            Util.getPlatform().openUri(page);
        } catch (RuntimeException e) {
            System.err.println("[ATT2] Could not open the quest guide: " + e.getMessage());
        }
    }

    private URI guidePage() {
        QuestInfo selected = QuestModel.get().findByKey(selectedKey);
        if (selected == null) return null;
        int number = selected.isMain() ? QuestModel.get().getMainStep() : selected.number();
        return GuideLinks.page(selected.type(), number, selected.city(), ModLanguageManager.getCurrentLanguage());
    }

    /** Click a quest: show it on the left page (asks the map for its current objective). */
    public void selectQuest(QuestInfo quest) {
        if (!quest.key().equals(selectedKey)) {
            leftScroll = 0;
        }
        selectedKey = quest.key();
        QuestModel.get().setSelected(quest);
        QuestModel.get().requestDetails(quest);
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshGuideTooltip();
        super.render(graphics, mouseX, mouseY, partialTick);

        hovered = null;
        for (GuiEventListener child : this.children()) {
            if (child instanceof QuestBookWidget widget && widget.isMouseOver(mouseX, mouseY)) {
                hovered = widget;
            }
        }

        renderLeftPage(graphics);
        renderRightPageInfo(graphics);

        if (hovered != null) {
            List<Component> lines = hovered.getTooltipLines();
            if (!lines.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (layout == null) return;
        QuestBookSkin.book(graphics, layout);
        graphics.pose().pushMatrix();
        graphics.pose().translate(layout.x() + 18, layout.y() + 9);
        graphics.pose().scale(1.35f, 1.35f);
        graphics.drawString(this.font, ModLanguageManager.get("quest_book.journal"), 0, 0, QuestBookSkin.CREAM, false);
        graphics.pose().popMatrix();
        graphics.drawString(this.font, "Across The Time II", layout.x() + 19, layout.y() + 24, QuestBookSkin.CREAM, false);
        graphics.drawString(this.font, ModLanguageManager.get("quest_book.objective"), layout.leftX(), layout.y() + 46,
                QuestBookSkin.MUTED, false);
        graphics.fill(layout.leftX(), layout.y() + 61, layout.leftX() + layout.contentWidth(), layout.y() + 62, QuestBookSkin.RULE);
        int sx = layout.rightX(), sy = layout.searchY(), sw = layout.contentWidth();
        graphics.fill(sx, sy, sx + sw, sy + 20, QuestBookSkin.RULE);
        graphics.fill(sx + 1, sy + 1, sx + sw - 1, sy + 19, 0xFFF9F1DE);
    }

    public float textScale() { return layout.textScale(); }
    private static final int SCROLLBAR_WIDTH = 5;
    private int leftViewX() { return layout.leftX(); }
    private int leftViewWidth() { return layout.contentWidth() - 8; }
    private int leftViewTop() { return layout.detailTop(); }
    private int leftViewBottom() { return layout.detailBottom(); }
    private int lineSpacing() { return (int) Math.ceil(this.font.lineHeight * textScale()) + 3; }

    private int leftMaxScroll() {
        return Math.max(0, leftContentHeight - (leftViewBottom() - leftViewTop()));
    }

    private void renderLeftPage(GuiGraphics graphics) {
        int textX = leftViewX();
        int textWidth = leftViewWidth();
        int top = leftViewTop();
        int bottom = leftViewBottom();
        int viewport = bottom - top;

        List<LeftBlock> blocks = buildLeftBlocks();
        leftContentHeight = measureBlocks(blocks, textWidth);
        leftScroll = Mth.clamp(leftScroll, 0, leftMaxScroll());

        graphics.enableScissor(textX, top, textX + textWidth, bottom);
        int y = top - leftScroll;
        for (LeftBlock block : blocks) {
            y = drawWrapped(graphics, block.text(), textX, y, textWidth, block.color()) + block.gapAfter();
        }
        graphics.disableScissor();

        if (leftMaxScroll() > 0) {
            drawLeftScrollbar(graphics, top, bottom, viewport);
        }
    }

    private record LeftBlock(Component text, int color, int gapAfter) {}

    private List<LeftBlock> buildLeftBlocks() {
        QuestModel model = QuestModel.get();
        QuestInfo selected = model.findByKey(selectedKey);
        HUDConfig config = HUDConfig.get();
        final int textColor = HUDConfig.color(config.questBookTextColor, 0xFF000000);
        final int secondary = HUDConfig.color(config.questBookSecondaryTextColor, 0xFF404040);
        List<LeftBlock> blocks = new ArrayList<>();

        if (selected == null) {
            blocks.add(new LeftBlock(ModLanguageManager.get("quest_book.description1"), textColor, 6));
            blocks.add(new LeftBlock(ModLanguageManager.get("quest_book.description2"), textColor, 0));
            return blocks;
        }

        Component header = Component.literal(selected.plainName()).withStyle(ChatFormatting.BOLD);
        blocks.add(new LeftBlock(header, QuestBookSkin.INK, 5));
        blocks.add(new LeftBlock(ModLanguageManager.get("quest_book.status." + selected.status().name().toLowerCase(Locale.ROOT)),
                QuestBookSkin.statusColor(selected.status()), 8));
        if (selected.isMain()) {
            int step = model.getMainStep();
            String progress = ModLanguageManager.format("quest_book.main_progress", "step", step < 0 ? "?" : step, "max", model.getMainMaxStep());
            blocks.add(new LeftBlock(Component.literal(progress), secondary, 0));
        } else if (selected.isDaily()) {
            Component city = Component.literal(ModLanguageManager.getString("quest_book.daily_city") + " ").append(selected.cityName());
            blocks.add(new LeftBlock(city, secondary, 0));
            if (selected.remainingSeconds() >= 0) {
                String time = ModLanguageManager.format("quest_book.daily_remaining", "time", selected.formatRemainingTime());
                blocks.add(new LeftBlock(Component.literal(time), secondary, 0));
            }
        }
        List<Component> description = model.getDescription(selected);
        if (description.isEmpty()) {
            Component hint = model.isCapturing()
                    ? ModLanguageManager.get("quest_book.loading")
                    : ModLanguageManager.get("quest_book.no_description");
            blocks.add(new LeftBlock(hint, secondary, 0));
            return blocks;
        }
        if (!blocks.isEmpty()) {
            LeftBlock last = blocks.remove(blocks.size() - 1);
            blocks.add(new LeftBlock(last.text(), last.color(), last.gapAfter() + 4));
        }
        for (int i = 0; i < description.size(); i++) {
            int gap = i == description.size() - 1 ? 0 : 2;
            blocks.add(new LeftBlock(description.get(i), textColor, gap));
        }
        return blocks;
    }

    private int measureBlocks(List<LeftBlock> blocks, int width) {
        int height = 0;
        for (LeftBlock block : blocks) {
            height += this.font.split(block.text(), (int) (width / textScale())).size() * lineSpacing() + block.gapAfter();
        }
        return height;
    }

    private void drawLeftScrollbar(GuiGraphics graphics, int top, int bottom, int viewport) {
        int barX = leftViewX() + leftViewWidth() + 1;
        int trackH = bottom - top;
        int thumbH = Math.max(8, viewport * viewport / leftContentHeight);
        int maxThumbTravel = trackH - thumbH;
        int thumbY = top + (leftMaxScroll() == 0 ? 0 : leftScroll * maxThumbTravel / leftMaxScroll());
        graphics.fill(barX, top, barX + SCROLLBAR_WIDTH, bottom, 0x33000000);
        graphics.fill(barX, thumbY, barX + SCROLLBAR_WIDTH, thumbY + thumbH, 0x99000000);
    }

    private boolean isOverLeftText(double mouseX, double mouseY) {
        return mouseX >= leftViewX() && mouseX < leftViewX() + leftViewWidth() + SCROLLBAR_WIDTH + 2
                && mouseY >= leftViewTop() && mouseY < leftViewBottom();
    }

    private boolean isOverLeftScrollbar(double mouseX, double mouseY) {
        if (leftMaxScroll() <= 0) return false;
        int barX = leftViewX() + leftViewWidth() + 1;
        return mouseX >= barX - 1 && mouseX < barX + SCROLLBAR_WIDTH + 2
                && mouseY >= leftViewTop() && mouseY < leftViewBottom();
    }

    private void scrollLeftTo(double mouseY) {
        int top = leftViewTop();
        int bottom = leftViewBottom();
        int viewport = bottom - top;
        int thumbH = Math.max(8, viewport * viewport / Math.max(leftContentHeight, 1));
        int maxThumbTravel = Math.max(1, viewport - thumbH);
        double rel = (mouseY - top - thumbH / 2.0) / maxThumbTravel;
        leftScroll = Mth.clamp((int) Math.round(rel * leftMaxScroll()), 0, leftMaxScroll());
    }

    /** Wrap at the displayed font size; scrolling uses the same line spacing. */
    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int defaultColor) {
        for (FormattedCharSequence line : this.font.split(text, (int) (width / textScale()))) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(textScale(), textScale());
            graphics.drawString(this.font, line, 0, 0, defaultColor, false);
            graphics.pose().popMatrix();
            y += lineSpacing();
        }
        return y;
    }

    private void renderRightPageInfo(GuiGraphics graphics) {
        String pageInfo = (currentPage + 1) + " / " + (maxPage + 1);
        graphics.drawString(this.font, pageInfo,
                layout.rightX() + (layout.contentWidth() - this.font.width(pageInfo)) / 2,
                layout.footerY() + 6, QuestBookSkin.INK, false);
        QuestModel model = QuestModel.get();
        String counter = ModLanguageManager.format("quest_book.progress_short", "done", model.getSideCompleted(), "total", model.getSideTotal());
        graphics.drawString(this.font, counter, layout.leftX(), layout.footerY() + 6, QuestBookSkin.MUTED, false);
        if (elements.isEmpty()) {
            Component hint = model.isWaitingForSideQuests() || model.isWaitingForDailyQuests()
                    ? ModLanguageManager.get("quest_book.loading") : ModLanguageManager.get("quest_book.empty_short");
            graphics.enableScissor(layout.rightX(), layout.listTop(), layout.rightX() + layout.contentWidth(), layout.footerY() - 6);
            drawWrapped(graphics, hint, layout.rightX() + 4, layout.listTop() + 5, layout.contentWidth() - 8, QuestBookSkin.MUTED);
            graphics.disableScissor();
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY != 0 && isOverLeftText(mouseX, mouseY) && leftMaxScroll() > 0) {
            int step = Math.max(this.font.lineHeight, (int) Math.round(Math.abs(deltaY) * this.font.lineHeight));
            leftScroll = Mth.clamp(leftScroll - (int) Math.signum(deltaY) * step, 0, leftMaxScroll());
            return true;
        }
        if (deltaY != 0 && mouseX >= layout.rightX() && mouseX <= layout.rightX() + layout.contentWidth()
                && mouseY >= layout.listTop() && mouseY <= layout.footerY()) {
            setCurrentPage(currentPage - (int) Math.signum(deltaY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOverLeftScrollbar(event.x(), event.y())) {
            draggingLeftScroll = true;
            scrollLeftTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingLeftScroll && event.button() == 0) {
            scrollLeftTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingLeftScroll) {
            draggingLeftScroll = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.keyPressed(event) || super.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN) { setCurrentPage(currentPage + 1); return true; }
        if (event.key() == GLFW.GLFW_KEY_PAGE_UP) { setCurrentPage(currentPage - 1); return true; }
        return super.keyPressed(event);
    }
}
