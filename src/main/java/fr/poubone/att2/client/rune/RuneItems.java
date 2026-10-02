package fr.poubone.att2.client.rune;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Optional;

/** Map 1.1.1 item stacks for Codex icons / vanilla tooltips. No I/O. */
public final class RuneItems {
    private RuneItems() {
    }

    public static ItemStack rune(RuneCatalog.RuneDef rune) {
        ItemStack stack = new ItemStack(Items.GLOWSTONE_DUST);
        CompoundTag tag = new CompoundTag();
        tag.putString("EquipmentType", "rune");
        tag.putString("Rarity", "spe");
        tag.putByte("Runelvl", (byte) rune.level());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(10000001f + rune.level()), List.of(), List.of(), List.of()));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(rune.translationKey()));
        return stack;
    }

    public static ItemStack word(RuneCatalog.WordDef word) {
        ItemStack stack = new ItemStack(Items.PRIZE_POTTERY_SHERD);
        CompoundTag tag = new CompoundTag();
        tag.putString("EquipmentType", "runic_word");
        tag.putString("Rarity", "leg");
        tag.putString("RuneWord", String.valueOf(word.wordId()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(word.nameKey()));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    public static ItemStack arrow(RuneCatalog.ArrowDef arrow) {
        ItemStack stack = specialArrow(arrow);
        stack.setCount(Math.max(1, arrow.resultCount()));
        return stack;
    }

    /** Vanilla misc arrow counted as {@code #arrow} by the map hopper recipes. */
    public static ItemStack miscArrow() {
        ItemStack stack = new ItemStack(Items.ARROW);
        CompoundTag tag = new CompoundTag();
        tag.putString("EquipmentType", "arrow");
        tag.putString("Rarity", "misc");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /**
     * Base arrow stack required to craft {@code arrow}: 4× misc (tier 1) or 4× prior special (tier 2/3).
     * Count is set to {@link RuneCatalog.ArrowDef#baseArrowCount()}.
     */
    public static ItemStack craftBaseArrow(RuneCatalog.ArrowDef arrow) {
        ItemStack stack;
        if (arrow.usesVanillaBaseArrow()) {
            stack = miscArrow();
        } else {
            RuneCatalog.ArrowDef prior = RuneCatalog.arrowByNameKey(arrow.priorArrowKey()).orElse(null);
            stack = prior != null ? specialArrow(prior) : miscArrow();
        }
        stack.setCount(arrow.baseArrowCount());
        return stack;
    }

    public static ItemStack specialArrow(RuneCatalog.ArrowDef arrow) {
        ItemStack stack = new ItemStack(Items.ARROW);
        CompoundTag tag = new CompoundTag();
        tag.putString("EquipmentType", "arrow");
        tag.putString("Rarity", rarityForArrowId(arrow.id()));
        tag.putBoolean("special_arrow", true);
        putSpecialArrowLevel(tag, arrow.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(arrow.nameKey()));
        return stack;
    }

    /** True if {@code stack} matches the map recipe base counted for this craft. */
    public static boolean matchesCraftBaseArrow(ItemStack stack, RuneCatalog.ArrowDef craft) {
        if (stack == null || stack.isEmpty() || craft == null) {
            return false;
        }
        return matchesArrowPrototype(stack, craftBaseArrow(craft));
    }

    public static int countCraftBaseArrows(LocalPlayer player, RuneCatalog.ArrowDef craft) {
        if (player == null || craft == null) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (matchesCraftBaseArrow(stack, craft)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    static boolean matchesArrowPrototype(ItemStack stack, ItemStack prototype) {
        if (stack.getItem() != prototype.getItem()) {
            return false;
        }
        CustomData protoData = prototype.get(DataComponents.CUSTOM_DATA);
        CustomData candData = stack.get(DataComponents.CUSTOM_DATA);
        if (protoData == null || protoData.isEmpty()) {
            return false;
        }
        if (candData == null || candData.isEmpty()) {
            return false;
        }
        CompoundTag pt = protoData.copyTag();
        CompoundTag ct = candData.copyTag();
        if (!"arrow".equals(ct.getString("EquipmentType").orElse(""))) {
            return false;
        }
        if ("misc".equals(pt.getString("Rarity").orElse(""))) {
            return "misc".equals(ct.getString("Rarity").orElse(""));
        }
        for (String key : List.of("explosive_arrow", "poisoned_arrow", "tracking_arrow")) {
            int expected = pt.getIntOr(key, -1);
            if (expected >= 0) {
                return ct.getIntOr(key, -2) == expected;
            }
        }
        Component protoName = prototype.get(DataComponents.CUSTOM_NAME);
        Component candName = stack.get(DataComponents.CUSTOM_NAME);
        return protoName != null && protoName.equals(candName);
    }

    private static void putSpecialArrowLevel(CompoundTag tag, String arrowId) {
        int underscore = arrowId.lastIndexOf('_');
        if (underscore <= 0 || underscore >= arrowId.length() - 1) {
            return;
        }
        String family = arrowId.substring(0, underscore);
        int tier;
        try {
            tier = Integer.parseInt(arrowId.substring(underscore + 1));
        } catch (NumberFormatException ex) {
            return;
        }
        tag.putInt(family + "_arrow", tier);
    }

    private static String rarityForArrowId(String arrowId) {
        int underscore = arrowId.lastIndexOf('_');
        if (underscore < 0 || underscore >= arrowId.length() - 1) {
            return "unc";
        }
        try {
            int tier = Integer.parseInt(arrowId.substring(underscore + 1));
            return switch (tier) {
                case 2 -> "rar";
                case 3 -> "epi";
                default -> "unc";
            };
        } catch (NumberFormatException ex) {
            return "unc";
        }
    }

    public static ItemStack powder() {
        ItemStack stack = new ItemStack(Items.GLOWSTONE_DUST);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("att2.runes.recipe.runepowder"));
        return stack;
    }

    public static ItemStack esc() {
        ItemStack stack = new ItemStack(Items.QUARTZ);
        CompoundTag tag = new CompoundTag();
        tag.putString("Coin", "esc");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.recipe.esc"));
        return stack;
    }

    public static ItemStack chronoton() {
        ItemStack stack = new ItemStack(Items.GOLD_NUGGET);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("att2.shop.chronotons.buy"));
        return stack;
    }

    public static ItemStack darkResin() {
        return named(Items.PLAYER_HEAD, "item.quest.dark_resin.name");
    }

    public static ItemStack runicOre() {
        return named(Items.RAW_IRON, "item.coin.runic_ore.name");
    }

    /** Chest icon for inventory counts in the Codex stock column. */
    public static ItemStack inventoryIcon() {
        return new ItemStack(Items.CHEST);
    }

    /** Map rune bundle (the in-game pouch) for Codex stock counts. */
    public static ItemStack pouchIcon() {
        ItemStack stack = new ItemStack(Items.ENDER_EYE);
        CompoundTag tag = new CompoundTag();
        tag.putString("EquipmentType", "rune_bundle");
        tag.putString("Rarity", "misc");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(1f), List.of(), List.of(), List.of()));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    public static ItemStack named(Item item, String translationKey) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(translationKey));
        return stack;
    }

    public static ItemStack other(RuneCatalog.InfoDef info) {
        return switch (info.id()) {
            case "esc_2", "esc_7", "esc_25" -> {
                ItemStack stack = esc();
                int n = Integer.parseInt(info.id().substring(4));
                stack.setCount(n);
                yield stack;
            }
            case "xp" -> named(Items.EXPERIENCE_BOTTLE, info.nameKey());
            case "reforge" -> named(Items.ANVIL, info.nameKey());
            case "loot_runes" -> named(Items.GLOWSTONE_DUST, info.nameKey());
            case "extraloot" -> named(Items.CHEST, info.nameKey());
            case "elixir" -> named(Items.POTION, info.nameKey());
            case "rune_bundle" -> {
                ItemStack stack = pouchIcon();
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable(info.nameKey()));
                yield stack;
            }
            case "spell_bundle_1", "spell_bundle_2", "spell_bundle_3" -> named(Items.BLUE_BUNDLE, info.nameKey());
            default -> named(Items.BOOK, info.nameKey());
        };
    }

    /** First inventory stack that matches the Codex prototype (for real map lore/tooltips). */
    public static Optional<ItemStack> inventoryMatch(ItemStack prototype) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || prototype == null || prototype.isEmpty()) {
            return Optional.empty();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!candidate.isEmpty() && codexStacksMatch(prototype, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean codexStacksMatch(ItemStack prototype, ItemStack candidate) {
        if (prototype.getItem() != candidate.getItem()) {
            return false;
        }
        CustomData protoData = prototype.get(DataComponents.CUSTOM_DATA);
        CustomData candData = candidate.get(DataComponents.CUSTOM_DATA);
        if (protoData != null && !protoData.isEmpty()) {
            if (candData == null || candData.isEmpty()) {
                return false;
            }
            CompoundTag pt = protoData.copyTag();
            CompoundTag ct = candData.copyTag();
            String equipmentType = pt.getString("EquipmentType").orElse("");
            if (!equipmentType.isEmpty()) {
                if (!equipmentType.equals(ct.getString("EquipmentType").orElse(""))) {
                    return false;
                }
                if ("rune".equals(equipmentType)) {
                    return pt.getByteOr("Runelvl", (byte) -1) == ct.getByteOr("Runelvl", (byte) -1);
                }
                if ("runic_word".equals(equipmentType)) {
                    return pt.getString("RuneWord").orElse("").equals(ct.getString("RuneWord").orElse(""));
                }
            }
            String coin = pt.getString("Coin").orElse("");
            if (!coin.isEmpty()) {
                return coin.equals(ct.getString("Coin").orElse(""));
            }
        }
        Component protoName = prototype.get(DataComponents.CUSTOM_NAME);
        Component candName = candidate.get(DataComponents.CUSTOM_NAME);
        if (protoName != null && candName != null) {
            return protoName.equals(candName);
        }
        return prototype.getCount() == candidate.getCount();
    }
}
