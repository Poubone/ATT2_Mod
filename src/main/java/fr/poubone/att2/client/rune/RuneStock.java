package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.data.ScoreCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuneStock {
    public record Counts(int inventory, OptionalInt pouch) {
    }

    private static final Pattern LAST_INT = Pattern.compile("(\\d+)(?!.*\\d)");

    private RuneStock() {
    }

    public static Counts counts(LocalPlayer player, RuneCatalog.RuneDef rune) {
        return new Counts(countInventory(player, rune), pouch(player, rune));
    }

    public static int countInventory(LocalPlayer player, RuneCatalog.RuneDef rune) {
        if (player == null) return 0;
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            total += countStack(player.getInventory().getItem(i), rune);
        }
        return total;
    }

    public static OptionalInt pouch(LocalPlayer player, RuneCatalog.RuneDef rune) {
        OptionalInt live = ScoreCache.get(rune.scoreObjective());
        if (live.isPresent()) return live;
        return loreFallback(player, rune);
    }

    public static OptionalInt powder() {
        return ScoreCache.getHolder("RUNE_POWDER", "#stock");
    }

    public static int esc(LocalPlayer player) {
        if (player == null) return 0;
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            total += countEsc(player.getInventory().getItem(i));
        }
        return total;
    }

    public static boolean unlocked(RuneCatalog.RuneDef rune) {
        return ScoreCache.getHolder("RUNE", rune.unlockHolder()).orElse(0) >= 1;
    }

    public static OptionalInt price(RuneCatalog.RuneDef rune) {
        return ScoreCache.getHolder("PRICES", rune.priceHolder());
    }

    public static OptionalInt escPrice(RuneCatalog.RuneDef rune) {
        return ScoreCache.getHolder("PRICES", rune.escPriceHolder());
    }

    public static OptionalInt bonus(String holder) {
        return ScoreCache.getHolder("RUNE", holder);
    }

    static int countStack(ItemStack stack, RuneCatalog.RuneDef rune) {
        if (stack == null || stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return 0;
        CompoundTag tag = data.copyTag();
        if (!"rune".equals(tag.getString("EquipmentType").orElse(""))) return 0;
        if (tag.getByteOr("Runelvl", (byte) -1) != (byte) rune.level()) return 0;
        return stack.getCount();
    }

    static int countEsc(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return 0;
        CompoundTag tag = data.copyTag();
        if (!"esc".equals(tag.getString("Coin").orElse(""))) return 0;
        return stack.getCount();
    }

    static boolean isBundle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return false;
        return "rune_bundle".equals(data.copyTag().getString("EquipmentType").orElse(""));
    }

    static OptionalInt loreFallback(LocalPlayer player, RuneCatalog.RuneDef rune) {
        if (player == null) return OptionalInt.empty();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            OptionalInt parsed = parseBundleLore(player.getInventory().getItem(i), rune);
            if (parsed.isPresent()) return parsed;
        }
        return OptionalInt.empty();
    }

    static OptionalInt parseBundleLore(ItemStack stack, RuneCatalog.RuneDef rune) {
        if (!isBundle(stack)) return OptionalInt.empty();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return OptionalInt.empty();
        String needle = "att2.rune_bundle.total." + rune.id();
        for (Component line : lore.lines()) {
            OptionalInt resolved = translatedValue(line, needle);
            if (resolved.isPresent()) return resolved;
        }
        return OptionalInt.empty();
    }

    private static OptionalInt translatedValue(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (translated.getKey().equals(key) && translated.getArgs().length > 0) {
                Object arg = translated.getArgs()[0];
                if (!(arg instanceof Component nested && nested.getContents() instanceof ScoreContents)) {
                    String text = arg instanceof Component value ? value.getString() : String.valueOf(arg);
                    Matcher matcher = LAST_INT.matcher(text);
                    if (matcher.find()) return OptionalInt.of(Integer.parseInt(matcher.group(1)));
                }
            }
            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    OptionalInt found = translatedValue(nested, key);
                    if (found.isPresent()) return found;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            OptionalInt found = translatedValue(sibling, key);
            if (found.isPresent()) return found;
        }
        return OptionalInt.empty();
    }
}
