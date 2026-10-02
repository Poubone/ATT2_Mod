package fr.poubone.att2.client.rune;

public final class CraftQuantity {
    public static final int[] PRESETS = {1, 10, 32, 64};

    private CraftQuantity() {}

    public static int maxSynth(int powderHave, int powderNeed, int escHave, int escNeed) {
        if (powderNeed <= 0) return 0;
        if (escNeed <= 0) return powderHave / powderNeed;
        return Math.min(powderHave / powderNeed, escHave / escNeed);
    }

    public static int maxWords(int[] invCounts, int[] needsPerCraft) {
        if (invCounts == null || needsPerCraft == null || invCounts.length != needsPerCraft.length
                || invCounts.length == 0) {
            return 0;
        }
        int max = Integer.MAX_VALUE;
        for (int i = 0; i < invCounts.length; i++) {
            if (needsPerCraft[i] <= 0) return 0;
            max = Math.min(max, invCounts[i] / needsPerCraft[i]);
        }
        return max == Integer.MAX_VALUE ? 0 : Math.max(0, max);
    }

    public static int maxArrows(int baseHave, int baseNeed, int[] runeHave, int[] runeNeed,
                               int hopperSlots, int stackSize) {
        if (baseNeed <= 0 || stackSize <= 0 || hopperSlots <= 0) return 0;
        if (runeHave == null || runeNeed == null || runeHave.length != runeNeed.length) return 0;
        int invMax = baseHave / baseNeed;
        for (int i = 0; i < runeNeed.length; i++) {
            if (runeNeed[i] <= 0) return 0;
            invMax = Math.min(invMax, runeHave[i] / runeNeed[i]);
        }
        int lo = 0, hi = invMax;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (hopperFits(mid, baseNeed, runeNeed, hopperSlots, stackSize)) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private static boolean hopperFits(int qty, int baseNeed, int[] runeNeed,
                                      int hopperSlots, int stackSize) {
        int slots = ceilDiv(qty * baseNeed, stackSize);
        for (int need : runeNeed) {
            slots += ceilDiv(qty * need, stackSize);
        }
        return slots <= hopperSlots;
    }

    private static int ceilDiv(int num, int den) {
        if (num <= 0) return 0;
        return (num + den - 1) / den;
    }

    public static boolean presetEnabled(int preset, int max) {
        return preset > 0 && preset <= max;
    }

    /** presetOrZeroForAll: 0 = Tout. Returns 0 if choice impossible. */
    public static int resolveChoice(int presetOrZeroForAll, int max) {
        if (max <= 0) return 0;
        if (presetOrZeroForAll == 0) return max;
        return presetEnabled(presetOrZeroForAll, max) ? presetOrZeroForAll : 0;
    }
}
