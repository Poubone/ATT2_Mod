package fr.poubone.att2.client.shop;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Visual identity of a merchant stall. Inferred from the map's category headers and the items it sent.
 */
public enum ShopType {
    BLACKSMITH("blacksmith", 0xFFE8B86A, 24, 20, 24, 20),
    FOOD("food", 0xFFF2D4A3, 24, 20, 24, 20),
    ALCHEMIST("alchemist", 0xFFB6F0C8, 24, 20, 24, 20),
    FLETCHER("fletcher", 0xFFD5E8B0, 24, 20, 24, 20),
    DAHAL("dahal", 0xFFC9B8FF, 24, 20, 24, 20),
    TAILOR("tailor", 0xFFF0C4D4, 24, 20, 24, 20),
    FISH("fish", 0xFF9ED4D8, 24, 20, 24, 20),
    STABLE("stable", 0xFFE6C48A, 24, 20, 24, 20),
    RUNES("runes", 0xFF94DEEE, 24, 20, 24, 20),
    CHARLES("charles", 0xFFE6BE70, 24, 20, 24, 20),
    MINER("miner", 0xFFAFC8CF, 24, 20, 24, 20),
    GENERAL("general", 0xFFE8D5A8, 24, 20, 24, 20);

    public final String id;
    public final int titleColor;
    public final int insetLeft;
    public final int insetTop;
    public final int insetRight;
    public final int insetBottom;

    ShopType(String id, int titleColor, int insetLeft, int insetTop, int insetRight, int insetBottom) {
        this.id = id;
        this.titleColor = titleColor;
        this.insetLeft = insetLeft;
        this.insetTop = insetTop;
        this.insetRight = insetRight;
        this.insetBottom = insetBottom;
    }

    public String titleKey() {
        return "shop.type." + id;
    }

    static ShopType infer(ShopCatalog catalog) {
        int blacksmith = 0, food = 0, potion = 0, bow = 0, spell = 0, cloth = 0, fish = 0, ride = 0, misc = 0;
        for (String category : catalog.categories()) {
            String key = normalize(category);
            if (containsAny(key, "arme", "weapon", "armure", "armor", "smith", "forge")) blacksmith += 3;
            else if (containsAny(key, "nourriture", "food", "comida", "nahrung")) food += 3;
            else if (containsAny(key, "potion", "alchim")) potion += 3;
            else if (containsAny(key, "arc", "bow", "fleche", "arrow")) bow += 3;
            else if (containsAny(key, "sort", "spell", "dahal", "magie", "perfection", "enhancement",
                    "aprimor", "verbesser", "усилен", "mejora")) spell += 3;
            else if (containsAny(key, "vetement", "cloth", "habit", "tailor")) cloth += 3;
            else if (containsAny(key, "poisson", "fish", "peche", "fishing")) fish += 3;
            else if (containsAny(key, "monture", "ride", "horse", "cheval")) ride += 3;
            else misc += 1;
        }
        for (ShopOffer offer : catalog.offers()) {
            switch (kindOf(offer)) {
                case WEAPON, ARMOR -> blacksmith += 2;
                case FOOD -> food += 2;
                case POTION -> potion += 2;
                case BOW -> bow += 2;
                case SPELL -> spell += 2;
                case CLOTH -> cloth += 2;
                case FISH -> fish += 2;
                case RIDE -> ride += 2;
                default -> misc += 1;
            }
        }
        if (catalog.hasMending()) blacksmith += 6;
        boolean bowStall = hasCategory(catalog, "arc", "bow", "fleche", "arrow")
                && !hasCategory(catalog, "armure", "armor");
        if (bowStall) bow += 12;
        int best = blacksmith;
        ShopType type = blacksmith > 0 ? BLACKSMITH : GENERAL;
        if (spell > best) { best = spell; type = DAHAL; }
        if (potion > best) { best = potion; type = ALCHEMIST; }
        if (bow > best && (bowStall || blacksmith < bow + 2)) { best = bow; type = FLETCHER; }
        if (cloth > best) { best = cloth; type = TAILOR; }
        if (fish > best) { best = fish; type = FISH; }
        if (ride > best) { best = ride; type = STABLE; }
        if (food > best) { type = FOOD; }
        if (bowStall && type == BLACKSMITH) return FLETCHER;
        if (best == 0 && type == GENERAL) return GENERAL;
        return type;
    }

    enum Kind { WEAPON, ARMOR, FOOD, POTION, BOW, SPELL, CLOTH, FISH, RIDE, OTHER }

    static Kind kindOf(ShopOffer offer) {
        String category = normalize(offer.category());
        if (containsAny(category, "arme", "weapon")) return Kind.WEAPON;
        if (containsAny(category, "armure", "armor")) return Kind.ARMOR;
        if (containsAny(category, "nourriture", "food")) return Kind.FOOD;
        if (containsAny(category, "potion")) return Kind.POTION;
        if (containsAny(category, "arc", "bow")) return Kind.BOW;
        if (containsAny(category, "sort", "spell", "perfection", "enhancement")) return Kind.SPELL;
        if (containsAny(category, "vetement", "cloth", "habit")) return Kind.CLOTH;
        if (containsAny(category, "poisson", "fish")) return Kind.FISH;
        if (containsAny(category, "monture", "ride", "horse")) return Kind.RIDE;

        String equipment = normalize(offer.equipmentType());
        if (equipment.contains("range") || equipment.contains("bow")) return Kind.BOW;

        String key = normalize(offer.translationKey());
        if (key.contains("bow") || key.contains("arrow") || key.contains("fleche")) return Kind.BOW;
        if (key.startsWith("weapon") || key.contains("melee")) return Kind.WEAPON;
        if (key.startsWith("armor") || key.contains("armure")) return Kind.ARMOR;
        if (key.contains("spell") || key.contains("sort") || key.contains("dahal")) return Kind.SPELL;
        if (key.contains("potion")) return Kind.POTION;
        if (key.contains("shop.apple") || key.contains("shop.bread") || key.contains("shop.food")
                || key.contains("shop.beef") || key.contains("shop.pork") || key.contains("shop.chicken")
                || key.contains("shop.rabbit") || key.contains("shop.mutton")
                || key.contains("carrot") || key.contains("cookie") || key.contains("potato")
                || key.contains("pie") || key.contains("stew") || key.contains("pumpkin")
                || key.contains("beetroot") || key.contains("kelp")) return Kind.FOOD;
        if (key.contains("cloth") || key.contains("habit")) return Kind.CLOTH;
        if (key.contains("fish") || key.contains("poisson")) return Kind.FISH;
        if (key.contains("horse") || key.contains("ride") || key.contains("camel")
                || key.contains("pig") || key.contains("mule")) return Kind.RIDE;

        if (equipment.contains("melee") || equipment.contains("weapon")) return Kind.WEAPON;
        if (equipment.contains("armor")) return Kind.ARMOR;
        if (equipment.contains("spell")) return Kind.SPELL;

        ItemStack stack = offer.stack();
        if (stack.is(Items.APPLE) || stack.is(Items.BREAD) || stack.is(Items.CARROT) || stack.is(Items.COOKIE)
                || stack.is(Items.POTATO) || stack.is(Items.BAKED_POTATO) || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.MUSHROOM_STEW) || stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP)
                || stack.is(Items.CHICKEN) || stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.MUTTON) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.BEETROOT) || stack.is(Items.BEETROOT_SOUP) || stack.is(Items.DRIED_KELP)
                || stack.is(Items.MELON_SLICE) || stack.is(Items.PUMPKIN)) {
            return Kind.FOOD;
        }
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.GLASS_BOTTLE)) {
            return Kind.POTION;
        }
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW)
                || stack.is(Items.TIPPED_ARROW)) {
            return Kind.BOW;
        }
        if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.COOKED_COD) || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH) || stack.is(Items.FISHING_ROD)) {
            return Kind.FISH;
        }
        if (stack.is(Items.SADDLE) || stack.is(Items.LEATHER_HORSE_ARMOR) || stack.is(Items.IRON_HORSE_ARMOR)
                || stack.is(Items.GOLDEN_HORSE_ARMOR) || stack.is(Items.DIAMOND_HORSE_ARMOR)
                || stack.is(Items.CAMEL_SPAWN_EGG) || stack.is(Items.HORSE_SPAWN_EGG)
                || stack.is(Items.ENCHANTED_BOOK) && key.contains("horse")) {
            return Kind.RIDE;
        }
        if (stack.is(Items.ENCHANTED_BOOK) && (key.contains("spell") || pathContainsSpell(offer))) {
            return Kind.SPELL;
        }
        if (stack.is(Items.LEATHER) || stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)) {
            return Kind.CLOTH;
        }
        return Kind.OTHER;
    }

    private static boolean pathContainsSpell(ShopOffer offer) {
        return offer.sprite() != null && offer.sprite().getPath().contains("spell");
    }

    private static String normalize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            switch (c) {
                case 'é', 'è', 'ê', 'ë' -> out.append('e');
                case 'à', 'â', 'ä' -> out.append('a');
                case 'ù', 'û', 'ü' -> out.append('u');
                case 'ô', 'ö' -> out.append('o');
                case 'î', 'ï' -> out.append('i');
                case 'ç' -> out.append('c');
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean hasCategory(ShopCatalog catalog, String... needles) {
        for (String category : catalog.categories()) {
            if (containsAny(normalize(category), needles)) return true;
        }
        return false;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }
}
