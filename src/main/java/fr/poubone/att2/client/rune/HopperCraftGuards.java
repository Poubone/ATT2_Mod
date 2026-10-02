package fr.poubone.att2.client.rune;

/** Pure readiness checks for workshop hopper auto-craft (no GUI). */
public final class HopperCraftGuards {
    public enum Outcome { READY, MISSING_INV, POUCH_ONLY, HOPPER_OCCUPIED, OUT_OF_RANGE }

    private HopperCraftGuards() {
    }

    public static Outcome evaluate(boolean inRange, boolean hopperEmpty,
                                   boolean invSatisfied, boolean pouchCanCover) {
        if (!inRange) return Outcome.OUT_OF_RANGE;
        if (!hopperEmpty) return Outcome.HOPPER_OCCUPIED;
        if (invSatisfied) return Outcome.READY;
        if (pouchCanCover) return Outcome.POUCH_ONLY;
        return Outcome.MISSING_INV;
    }

    /**
     * Arrow crafts need base arrows in inventory (never in the rune pouch).
     * Missing arrows → {@link Outcome#MISSING_INV}; pouch tip only when arrows are OK but runes are pouch-only.
     */
    public static Outcome evaluateArrowCraft(boolean inRange, boolean hopperEmpty,
                                             boolean arrowsInInv, boolean runeInvSatisfied,
                                             boolean runePouchCanCover) {
        if (!inRange) return Outcome.OUT_OF_RANGE;
        if (!hopperEmpty) return Outcome.HOPPER_OCCUPIED;
        if (!arrowsInInv) return Outcome.MISSING_INV;
        if (runeInvSatisfied) return Outcome.READY;
        if (runePouchCanCover) return Outcome.POUCH_ONLY;
        return Outcome.MISSING_INV;
    }
}
