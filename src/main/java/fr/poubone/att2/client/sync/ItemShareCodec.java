package fr.poubone.att2.client.sync;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** SNBT round-trip of an ItemStack for the party-sync API. */
public final class ItemShareCodec {
    public static final int MAX_SNBT_CHARS = 20_000;

    private ItemShareCodec() {
    }

    public static Optional<String> encode(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty() || registries == null) {
            return Optional.empty();
        }
        try {
            Tag tag = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                    .result()
                    .orElse(null);
            if (tag == null) {
                return Optional.empty();
            }
            String snbt = tag.toString();
            if (snbt.length() > MAX_SNBT_CHARS) {
                return Optional.empty();
            }
            return Optional.of(snbt);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static ItemStack decode(String snbt, HolderLookup.Provider registries) {
        if (snbt == null || snbt.isBlank() || snbt.length() > MAX_SNBT_CHARS || registries == null) {
            return ItemStack.EMPTY;
        }
        try {
            Tag tag = TagParser.parseCompoundFully(snbt);
            return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                    .result()
                    .orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static String itemId(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString();
    }

    public static String displayName(ItemStack stack) {
        return stack.getHoverName().getString();
    }
}
