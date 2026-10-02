package fr.poubone.att2.client.data;

import java.util.Map;

/**
 * ScoreTrigger ids of the grimoire's clickable "select level" links, extracted from the map's
 * trigger/function_list.mcfunction (remaster 1.1.1). For each spell the lvl1..lvlN triggers are
 * consecutive: trigger = base + (level - 1). The book's Refresh ({@code obtain}) is the next
 * integer ({@code base + levels}). Spells 21-23 only have 3 levels; spells 11, 20, 31, 32 and 40
 * use other systems and have no selectlvl trigger at all.
 */
public final class SpellSelectTriggers {
    private record Entry(int base, int levels) {
    }

    private static final Map<Integer, Entry> TABLE = Map.ofEntries(
            Map.entry(1, new Entry(1181, 10)),
            Map.entry(2, new Entry(1208, 10)),
            Map.entry(3, new Entry(1301, 10)),
            Map.entry(4, new Entry(1361, 10)),
            Map.entry(5, new Entry(1435, 10)),
            Map.entry(6, new Entry(1447, 10)),
            Map.entry(7, new Entry(1459, 10)),
            Map.entry(8, new Entry(1471, 10)),
            Map.entry(9, new Entry(1483, 10)),
            Map.entry(10, new Entry(1193, 10)),
            Map.entry(21, new Entry(1220, 3)),
            Map.entry(22, new Entry(1226, 3)),
            Map.entry(23, new Entry(1232, 3)),
            Map.entry(24, new Entry(1238, 10)),
            Map.entry(25, new Entry(1251, 10)),
            Map.entry(26, new Entry(1264, 10)),
            Map.entry(27, new Entry(1277, 10)),
            Map.entry(28, new Entry(1289, 10)),
            Map.entry(29, new Entry(3415, 10)),
            Map.entry(30, new Entry(1313, 10)),
            Map.entry(33, new Entry(3428, 10)),
            Map.entry(34, new Entry(1349, 10)),
            Map.entry(35, new Entry(3444, 10)),
            Map.entry(41, new Entry(1375, 10)),
            Map.entry(42, new Entry(1387, 10)),
            Map.entry(43, new Entry(1399, 10)),
            Map.entry(44, new Entry(1411, 10)),
            Map.entry(45, new Entry(1423, 10)),
            Map.entry(46, new Entry(3462, 10))
    );

    private SpellSelectTriggers() {
    }

    public static boolean isSupported(int spellId) {
        return TABLE.containsKey(spellId);
    }

    /** Number of selectable levels for the spell, 0 when it has no selectlvl triggers. */
    public static int levelCount(int spellId) {
        Entry entry = TABLE.get(spellId);
        return entry == null ? 0 : entry.levels();
    }

    /** ScoreTrigger id selecting the given level, or -1 when the spell/level is out of the table. */
    public static int levelSelectTrigger(int spellId, int level) {
        Entry entry = TABLE.get(spellId);
        if (entry == null || level < 1 || level > entry.levels()) return -1;
        return entry.base() + level - 1;
    }

    /**
     * ScoreTrigger id of the grimoire's "Refresh" link ({@code action/spellN/obtain}).
     * In function_list this is always the integer right after the last selectlvl:
     * {@code base + levels}. Regenerates the written book with current XP scores.
     * Returns -1 when the spell has no selectlvl table.
     */
    public static int obtainTrigger(int spellId) {
        Entry entry = TABLE.get(spellId);
        return entry == null ? -1 : entry.base() + entry.levels();
    }
}
