package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.rune.RuneItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Map tellraw sprites and currency items onto the pack's real icons. */
final class ShopIcons {
    private static final Pattern SPELL_ID = Pattern.compile("(?:^|[._/])spell(\\d+)(?:[._/-]|$)");

    private ShopIcons() {
    }

    static ItemStack chronoton() {
        return RuneItems.chronoton();
    }

    static ItemStack esc() {
        return RuneItems.esc();
    }

    static ItemStack currency(boolean usesEsc) {
        return usesEsc ? esc() : chronoton();
    }

    static String vanillaItemPath(String spritePath) {
        if (spritePath == null) return null;
        String path = spritePath.toLowerCase(Locale.ROOT);
        if (!path.startsWith("item/")) return null;
        if (path.contains("custom") || path.contains("spell")) return null;
        String rest = path.substring("item/".length());
        if (rest.isEmpty() || rest.contains("/")) return null;
        return rest;
    }

    static boolean isPackTexture(String spritePath) {
        if (spritePath == null) return false;
        String path = spritePath.toLowerCase(Locale.ROOT);
        return path.contains("custom") || path.contains("spell")
                || path.contains("bait") || path.contains("fishing");
    }

    static boolean isPackTexture(Identifier sprite) {
        return sprite != null && isPackTexture(sprite.getPath());
    }

    static int spellId(String translationKey, String spritePath) {
        int fromKey = parseSpellId(translationKey);
        return fromKey > 0 ? fromKey : parseSpellId(spritePath);
    }

    static int spellId(String translationKey, Identifier sprite) {
        return spellId(translationKey, sprite == null ? null : sprite.getPath());
    }

    static int spellId(ShopOffer offer) {
        if (offer == null) return 0;
        int fromStack = spellIdFromStack(offer.stack());
        return fromStack > 0 ? fromStack : spellId(offer.translationKey(), offer.sprite());
    }

    static String launcherModelPath(int spellId) {
        return spellId > 0 ? "spell/" + spellId : null;
    }

    static boolean isLauncher(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && !data.isEmpty()
                && "launcher".equals(data.copyTag().getString("Dahal").orElse(""))) {
            return true;
        }
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        return model != null && parseSpellId(model.getPath()) > 0;
    }

    static ItemStack launcher(int spellId) {
        if (spellId <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putString("Dahal", "launcher");
        tag.putInt("Spell", spellId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace(launcherModelPath(spellId)));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(0f), List.of(), List.of(), List.of()));
        return stack;
    }

    private static int spellIdFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && !data.isEmpty()) {
            int id = data.copyTag().getIntOr("Spell", 0);
            if (id > 0) return id;
        }
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        return model == null ? 0 : parseSpellId(model.getPath());
    }

    private static int parseSpellId(String text) {
        if (text == null || text.isEmpty()) return 0;
        Matcher matcher = SPELL_ID.matcher(text.toLowerCase(Locale.ROOT));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    static ItemStack itemFromSprite(Identifier sprite) {
        if (sprite == null) return null;
        String itemPath = vanillaItemPath(sprite.getPath());
        if (itemPath == null) return null;
        Identifier id = Identifier.fromNamespaceAndPath(sprite.getNamespace(), itemPath);
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty() || item.get() == Items.AIR) return null;
        return new ItemStack(item.get());
    }

    static Identifier keepNamedSprite(Identifier sprite) {
        if (sprite == null) return null;
        if (isPackTexture(sprite) || vanillaItemPath(sprite.getPath()) != null) {
            return sprite;
        }
        return null;
    }

    static Component stripInlineObjects(Component component) {
        if (component == null) return Component.empty();
        if (component.getContents() instanceof ObjectContents) {
            MutableComponent out = Component.empty().withStyle(component.getStyle());
            for (Component sibling : component.getSiblings()) {
                out.append(stripInlineObjects(sibling));
            }
            return out;
        }
        MutableComponent copy;
        if (component.getContents() instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] cleaned = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                cleaned[i] = args[i] instanceof Component inner ? stripInlineObjects(inner) : args[i];
            }
            copy = Component.translatable(translatable.getKey(), cleaned).withStyle(component.getStyle());
        } else if (component.getContents() instanceof PlainTextContents) {
            copy = component.plainCopy().withStyle(component.getStyle());
        } else {
            copy = component.copy().withStyle(component.getStyle());
        }
        copy.getSiblings().clear();
        for (Component sibling : component.getSiblings()) {
            copy.append(stripInlineObjects(sibling));
        }
        return copy;
    }
}
