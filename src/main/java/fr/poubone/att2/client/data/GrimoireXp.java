package fr.poubone.att2.client.data;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.scores.ScoreHolder;

import java.util.OptionalInt;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the spell XP shown on the grimoire's stats page as "xp / next level threshold".
 * <p>
 * Reads the resolved written-book page after the player opens the grimoire.
 */
public final class GrimoireXp {
    public record XpState(int xp, int nextThreshold) {
    }

    private static final Pattern XP_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Map<Integer, XpState> CACHED_XP = new HashMap<>();
    private static final Map<Integer, Integer> CACHED_LEVEL = new HashMap<>();
    private static final Map<Integer, WrittenBookContent> OBSERVED_BOOKS = new HashMap<>();

    private GrimoireXp() {
    }

    /** True when the stack is this spell's written grimoire ({@code Dahal:book}). */
    public static boolean isGrimoire(ItemStack stack, int spellId) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return false;
        CompoundTag tag = data.copyTag();
        if (!"book".equals(tag.getString("Dahal").orElse(""))) return false;
        return tag.getIntOr("Spell", 0) == spellId;
    }

    /** Scans the player's inventory (and offhand) for the resolved grimoire page. */
    public static XpState read(LocalPlayer player, int spellId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            XpState state = parse(player.getInventory().getItem(slot), spellId, player);
            if (state != null) {
                CACHED_XP.put(spellId, state);
                return state;
            }
        }
        XpState offhand = parse(player.getOffhandItem(), spellId, player);
        if (offhand != null) CACHED_XP.put(spellId, offhand);
        return offhand != null ? offhand : CACHED_XP.get(spellId);
    }

    public static void remember(ItemStack stack, int spellId) {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null || !content.resolved()) return;
        if (OBSERVED_BOOKS.get(spellId) == content) return;
        OBSERVED_BOOKS.put(spellId, content);
        XpState xp = parse(stack, spellId);
        if (xp != null) CACHED_XP.put(spellId, xp);
        OptionalInt level = parseUnlockedLevel(stack, spellId);
        if (level.isPresent()) CACHED_LEVEL.put(spellId, level.getAsInt());
    }

    public static void invalidateXp(int spellId) {
        CACHED_XP.remove(spellId);
        OBSERVED_BOOKS.remove(spellId);
    }

    public static void reset() {
        CACHED_XP.clear();
        CACHED_LEVEL.clear();
        OBSERVED_BOOKS.clear();
    }

    /** Remember books the player opened manually, even after the map turns them back into launchers. */
    public static void captureResolvedBooks(LocalPlayer player) {
        if (player == null) return;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            captureResolvedBook(player.getInventory().getItem(slot));
        }
        captureResolvedBook(player.getOffhandItem());
    }

    private static void captureResolvedBook(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return;
        CompoundTag tag = data.copyTag();
        if (!"book".equals(tag.getString("Dahal").orElse(""))) return;
        int id = tag.getIntOr("Spell", 0);
        if (id > 0) remember(stack, id);
    }

    /** The book also states the highest unlocked level, once its page has been resolved. */
    public static OptionalInt readUnlockedLevel(LocalPlayer player, int spellId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            OptionalInt level = parseUnlockedLevel(player.getInventory().getItem(slot), spellId);
            if (level.isPresent()) {
                CACHED_LEVEL.put(spellId, level.getAsInt());
                return level;
            }
        }
        OptionalInt offhand = parseUnlockedLevel(player.getOffhandItem(), spellId);
        if (offhand.isPresent()) CACHED_LEVEL.put(spellId, offhand.getAsInt());
        return offhand.isPresent() ? offhand : CACHED_LEVEL.containsKey(spellId)
                ? OptionalInt.of(CACHED_LEVEL.get(spellId)) : OptionalInt.empty();
    }

    private static OptionalInt parseUnlockedLevel(ItemStack stack, int spellId) {
        if (!isGrimoire(stack, spellId)) return OptionalInt.empty();
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return OptionalInt.empty();
        for (Filterable<Component> page : content.pages()) {
            OptionalInt level = findUnlockedLevel(page.raw());
            if (level.isPresent()) return level;
            if (page.filtered().isPresent()) {
                level = findUnlockedLevel(page.filtered().get());
                if (level.isPresent()) return level;
            }
        }
        if (!content.pages().isEmpty()) {
            int expected = SpellSelectTriggers.levelCount(spellId);
            OptionalInt marked = countUnlockedMarkers(content.pages().getFirst().raw(), expected);
            if (marked.isPresent()) return marked;
            if (content.pages().getFirst().filtered().isPresent()) {
                return countUnlockedMarkers(content.pages().getFirst().filtered().get(), expected);
            }
        }
        return OptionalInt.empty();
    }

    /** The first page has one green or red dot per level after its NBT components resolve. */
    static OptionalInt countUnlockedMarkers(Component page, int expected) {
        if (expected <= 0) return OptionalInt.empty();
        int[] counts = new int[2];
        countMarkers(page, counts);
        return counts[0] + counts[1] == expected ? OptionalInt.of(counts[0]) : OptionalInt.empty();
    }

    private static void countMarkers(Component component, int[] counts) {
        if (component.getContents() instanceof PlainTextContents text && ".".equals(text.text())
                && component.getStyle().getColor() != null) {
            int color = component.getStyle().getColor().getValue();
            if (color == 0x55FF55) counts[0]++;
            if (color == 0xFF5555) counts[1]++;
        }
        for (Component sibling : component.getSiblings()) countMarkers(sibling, counts);
    }

    private static OptionalInt findUnlockedLevel(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if ("att2.item.dahal.book.spell_level".equals(translated.getKey())
                    && translated.getArgs().length > 0) {
                OptionalInt level = resolvedInteger(translated.getArgs()[0]);
                if (level.isPresent() && level.getAsInt() >= 0) return level;
            }
            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    OptionalInt level = findUnlockedLevel(nested);
                    if (level.isPresent()) return level;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            OptionalInt level = findUnlockedLevel(sibling);
            if (level.isPresent()) return level;
        }
        return OptionalInt.empty();
    }

    /** Null when the stack is not this spell's grimoire or its pages are not resolved yet. */
    public static XpState parse(ItemStack stack, int spellId) {
        return parse(stack, spellId, null);
    }

    private static XpState parse(ItemStack stack, int spellId, LocalPlayer player) {
        if (!isGrimoire(stack, spellId)) return null;
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return null;
        for (Filterable<Component> page : content.pages()) {
            XpState translated = parseProgress(page.raw());
            if (translated != null) return translated;
            XpState state = matchXp(flatten(page.raw(), player));
            if (state != null) return state;
            if (page.filtered().isPresent()) {
                translated = parseProgress(page.filtered().get());
                if (translated != null) return translated;
                state = matchXp(flatten(page.filtered().get(), player));
                if (state != null) return state;
            }
        }
        return null;
    }

    private static XpState parseProgress(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if ("att2.item.dahal.book.level_progress".equals(translated.getKey())
                    && translated.getArgs().length >= 2) {
                OptionalInt xp = resolvedInteger(translated.getArgs()[0]);
                OptionalInt threshold = resolvedInteger(translated.getArgs()[1]);
                if (xp.isPresent() && threshold.isPresent() && threshold.getAsInt() > 0) {
                    return new XpState(xp.getAsInt(), threshold.getAsInt());
                }
            }
            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    XpState found = parseProgress(nested);
                    if (found != null) return found;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            XpState found = parseProgress(sibling);
            if (found != null) return found;
        }
        return null;
    }

    private static OptionalInt resolvedInteger(Object value) {
        if (value instanceof Component component && component.getContents() instanceof ScoreContents) {
            return OptionalInt.empty();
        }
        String text = value instanceof Component component ? component.getString() : String.valueOf(value);
        Matcher matcher = Pattern.compile("-?\\d+").matcher(text);
        return matcher.find() ? OptionalInt.of(Integer.parseInt(matcher.group())) : OptionalInt.empty();
    }

    private static XpState matchXp(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher matcher = XP_PATTERN.matcher(text);
        if (!matcher.find()) return null;
        try {
            int xp = Integer.parseInt(matcher.group(1));
            int next = Integer.parseInt(matcher.group(2));
            if (next <= 0) return null;
            return new XpState(xp, next);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String flatten(Component component, LocalPlayer player) {
        StringBuilder sb = new StringBuilder();
        flatten(component, player, sb);
        return sb.toString();
    }

    private static void flatten(Component component, LocalPlayer player, StringBuilder sb) {
        var contents = component.getContents();
        if (contents instanceof ScoreContents score) {
            OptionalInt value = resolveScore(score, player);
            if (value.isPresent()) sb.append(value.getAsInt());
        } else if (contents instanceof PlainTextContents text) {
            sb.append(text.text());
        }
        for (Component sibling : component.getSiblings()) {
            flatten(sibling, player, sb);
        }
    }

    private static OptionalInt resolveScore(ScoreContents score, LocalPlayer player) {
        ScoreHolder holder = score.name().map(selector -> player, ScoreHolder::forNameOnly);
        if (holder == null) return OptionalInt.empty();
        return ScoreCache.readHolder(score.objective(), holder);
    }
}
