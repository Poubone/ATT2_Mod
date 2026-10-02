package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ATT2 shop NPCs keyed by {@code shop_opening} trigger and {@code att2.npc.name.*}.
 */
public final class ShopSellers {
    private static final UUID ELDRIC_UUID = UUID.fromString("00000000-0000-160a-0000-00000000160a");

    private static final Map<Integer, ShopSeller> BY_TRIGGER = new HashMap<>();
    private static final Map<String, ShopSeller> BY_NPC_ID = new HashMap<>();
    private static final Map<String, ShopSeller> BY_ALIAS = new HashMap<>();

    static {
        // Forges: armes + armures + réparation + reset
        smith(769, "wulk");
        smith(768, "vulk");
        smith(865, "ramsay_tork", "ramsay");
        smith(760, "rokar_borton", "rokar", "borton");
        smith(772, "zirthan");
        smith(729, "carmine_mordan", "carmine");
        smith(770, "xoltan_zahav", "xoltan");

        // Archerie: arcs, reset, pas de réparation
        seller(755, "maria", ShopType.FLETCHER, false, true);
        seller(735, "emera_palundra", ShopType.FLETCHER, false, true, "emera");

        // Alchimie
        seller(757, "oswald_flamel", ShopType.ALCHEMIST, false, true, "oswald");
        seller(765, "sylvia_mornith", ShopType.ALCHEMIST, false, true, "sylvia");
        seller(748, "jabir_hayyan", ShopType.ALCHEMIST, false, true, "jabir");

        // Épicerie / boucherie
        food(976, "carmen_ysta", "carmen");
        food(1048, "sigfrid_barkon", "sigfrid");
        food(758, "rena_aboth", "rena");
        food(767, "viserys_yigdal", "viserys");
        food(733, "elsa_rasmon", "elsa");
        food(753, "lisa_payin", "lisa");
        food(725, "alcimene");
        food(751, "kehmira_alzedria", "kehmira");
        food(1016, "alyia_lana", "alyia");
        food(759, "rick_palundra", "rick");
        food(738, "ethan_mordheim");
        food(0, "elisa_meli", "elisa");

        // Poissonnerie
        seller(750, "jano_grant", ShopType.FISH, false, false, "jano");
        seller(740, "felix_amori", ShopType.FISH, false, false, "felix");
        seller(739, "ethan_solg", ShopType.FISH, false, false);

        // Écuries
        seller(883, "helena_meli", ShopType.STABLE, false, false, "helena");
        seller(754, "marc_aboth", ShopType.STABLE, false, false, "marc");
        seller(731, "chris_amork", ShopType.STABLE, false, false, "chris");
        seller(736, "eric_melsath", ShopType.STABLE, false, false, "eric");

        // Dahäl
        seller(762, "stella", ShopType.DAHAL, false, false);
        seller(771, "yaakov_rav", ShopType.DAHAL, false, false, "yaakov");
        seller(727, "aramis", ShopType.DAHAL, false, false);

        // Couture / cuir
        seller(732, "chryses_aleria", ShopType.TAILOR, false, false, "chryses");
        seller(734, "elziel_salvidam", ShopType.TAILOR, false, false, "elziel");
        seller(743, "homer", ShopType.TAILOR, false, true);

        // Bazar
        seller(737, "estelle", ShopType.GENERAL, false, false);
        seller(1060, "patrick_corth", ShopType.GENERAL, false, false, "patrick");

        // ESC (même chrome forge, sans réparation ni reset)
        seller(761, "sirna_kho", ShopType.BLACKSMITH, false, false, "sirna");
        seller(749, "jade_rozaell", ShopType.BLACKSMITH, false, false, "jade");

        ShopSeller charles = new ShopSeller(730, "charles", ShopType.GENERAL, false, false, true);
        register(charles, "charles");

        register(new ShopSeller(0, "eldric", ShopType.MINER, false, false, false), "eldric");
    }

    private ShopSellers() {
    }

    public static ShopSeller byTrigger(int trigger) {
        return BY_TRIGGER.get(trigger);
    }

    public static boolean isShopOpening(int trigger) {
        ShopSeller seller = BY_TRIGGER.get(trigger);
        return seller != null && !seller.gambling();
    }

    public static boolean isEldric(Entity entity) {
        if (entity == null) return false;
        if (ELDRIC_UUID.equals(entity.getUUID())) return true;
        Component custom = entity.getCustomName();
        if (custom != null) {
            String key = translationKey(custom);
            if ("att2.npc.name.eldric".equals(key)) return true;
        }
        ShopSeller matched = matchEntity(entity);
        if (matched != null && "eldric".equals(matched.npcId())) return true;
        return "eldric".equals(normalize(entity.getName().getString()));
    }

    public static ShopSeller matchEntity(Entity entity) {
        if (entity == null) return null;
        Component custom = entity.getCustomName();
        if (custom != null) {
            String key = translationKey(custom);
            if (key.startsWith("att2.npc.name.")) {
                ShopSeller seller = BY_NPC_ID.get(key.substring("att2.npc.name.".length()));
                if (seller != null) return seller;
            }
        }
        return BY_ALIAS.get(normalize(entity.getName().getString()));
    }

    private static void smith(int trigger, String npcId, String... aliases) {
        seller(trigger, npcId, ShopType.BLACKSMITH, true, true, aliases);
    }

    private static void food(int trigger, String npcId, String... aliases) {
        seller(trigger, npcId, ShopType.FOOD, false, false, aliases);
    }

    private static void seller(int trigger, String npcId, ShopType type, boolean repair, boolean reset,
                               String... aliases) {
        ShopSeller seller = new ShopSeller(trigger, npcId, type, repair, reset, false);
        register(seller, aliases);
    }

    private static void register(ShopSeller seller, String... aliases) {
        if (seller.trigger() > 0) {
            BY_TRIGGER.put(seller.trigger(), seller);
        }
        BY_NPC_ID.put(seller.npcId(), seller);
        addAlias(seller.npcId(), seller);
        addAlias(seller.npcId().replace('_', ' '), seller);
        for (String alias : aliases) {
            addAlias(alias, seller);
        }
    }

    private static void addAlias(String alias, ShopSeller seller) {
        if (alias == null || alias.isBlank()) return;
        String key = normalize(alias);
        if (key.isEmpty() || BY_ALIAS.containsKey(key)) return;
        BY_ALIAS.put(key, seller);
    }

    private static String translationKey(Component component) {
        if (component == null) return "";
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        for (Component sibling : component.getSiblings()) {
            String inner = translationKey(sibling);
            if (!inner.isEmpty()) return inner;
        }
        return "";
    }

    static String normalize(String value) {
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
        return out.toString().strip();
    }
}
