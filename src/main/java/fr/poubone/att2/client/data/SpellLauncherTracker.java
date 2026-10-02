package fr.poubone.att2.client.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans the player's inventory for ATT2 spell launchers and tracks their cooldown state.
 * <p>
 * A launcher is an {@code enchanted_book} with {@code custom_data={Dahal:"launcher",Spell:N}}.
 * The map's datapack rewrites {@code custom_model_data.floats[0]} every tick:
 * 0 = ready, 1..10 = remaining cooldown in tenths (11 may appear briefly on obtain).
 */
public final class SpellLauncherTracker {
    public static final int FLASH_TICKS = 20;

    public static final class LauncherState {
        public final int spellId;
        public final ItemStack stack;
        /** Selected spell level parsed from the custom name ("lvlN"), 0 when unknown. */
        public final int level;
        /** 0 = ready, 1..10 = remaining cooldown in tenths of the full duration. */
        public final int cooldownTenths;
        public int flashTicks;

        LauncherState(int spellId, ItemStack stack, int level, int cooldownTenths) {
            this.spellId = spellId;
            this.stack = stack;
            this.level = level;
            this.cooldownTenths = cooldownTenths;
        }

        public boolean ready() {
            return cooldownTenths == 0;
        }
    }

    private static final Pattern LEVEL_PATTERN = Pattern.compile("lvl(\\d+)");

    private static List<LauncherState> states = List.of();
    /** spellId -> cooldownTenths of the previous tick, to detect the "just ready" transition. */
    private static final Map<Integer, Integer> lastTenths = new HashMap<>();
    /** spellId -> remaining flash ticks. */
    private static final Map<Integer, Integer> flash = new HashMap<>();

    private SpellLauncherTracker() {
    }

    public static List<LauncherState> states() {
        return states;
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }
        Map<Integer, LauncherState> found = new TreeMap<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            consider(found, player.getInventory().getItem(slot));
        }
        consider(found, player.getOffhandItem());

        for (LauncherState state : found.values()) {
            Integer previous = lastTenths.get(state.spellId);
            if (previous != null && previous > 0 && state.cooldownTenths == 0) {
                flash.put(state.spellId, FLASH_TICKS);
            }
            lastTenths.put(state.spellId, state.cooldownTenths);
            Integer remaining = flash.get(state.spellId);
            if (remaining != null && remaining > 0) {
                state.flashTicks = remaining;
                flash.put(state.spellId, remaining - 1);
            }
        }
        lastTenths.keySet().retainAll(found.keySet());
        flash.keySet().retainAll(found.keySet());
        states = List.copyOf(found.values());
    }

    private static void consider(Map<Integer, LauncherState> found, ItemStack stack) {
        LauncherState state = parse(stack);
        if (state != null) {
            found.putIfAbsent(state.spellId, state);
        }
    }

    /** Parses a stack into a launcher state; null when the stack is not an ATT2 spell launcher. */
    public static LauncherState parse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return null;
        CompoundTag tag = data.copyTag();
        if (!"launcher".equals(tag.getString("Dahal").orElse(""))) return null;
        int spellId = tag.getIntOr("Spell", 0);
        if (spellId <= 0) return null;

        int tenths = 0;
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null && !modelData.floats().isEmpty()) {
            tenths = Math.round(modelData.floats().get(0));
        }
        tenths = Math.max(0, Math.min(10, tenths));

        int level = 0;
        Matcher matcher = LEVEL_PATTERN.matcher(stack.getHoverName().getString());
        if (matcher.find()) {
            level = Integer.parseInt(matcher.group(1));
        }
        return new LauncherState(spellId, stack, level, tenths);
    }

    public static void reset() {
        states = List.of();
        lastTenths.clear();
        flash.clear();
    }
}
