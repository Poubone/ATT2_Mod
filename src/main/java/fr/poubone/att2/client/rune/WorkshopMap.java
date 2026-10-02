package fr.poubone.att2.client.rune;

/** ATT2 1.1.1 workshop hopper + craft interaction geometry. */
public final class WorkshopMap {
    public static final double REACH = 5.0;

    public static final int HOPPER_X = -5029;
    public static final int HOPPER_Y = 90;
    public static final int HOPPER_Z = -4958;
    public static final String HOPPER_NAME_KEY = "att2.runes.crafting";
    public static final int HOPPER_SLOTS = 5;

    /** Map summons interaction with this tag; advancement listens for player interact. */
    public static final String CRAFT_INTERACTION_TAG = "RuneRecipeTrigger";
    public static final double INTERACTION_X = -5028.5;
    public static final double INTERACTION_Y = 91.25;
    public static final double INTERACTION_Z = -4956.8;

    /**
     * Fixed UUID from map summon {@code UUID:[I;-5028,91,-4956,1]}
     * ({@code ffffec5c-0000-005b-ffff-eca400000001}).
     */
    public static final java.util.UUID CRAFT_INTERACTION_UUID =
            java.util.UUID.fromString("ffffec5c-0000-005b-ffff-eca400000001");

    private WorkshopMap() {
    }
}
