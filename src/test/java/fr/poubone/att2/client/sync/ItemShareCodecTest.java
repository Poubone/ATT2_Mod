package fr.poubone.att2.client.sync;

import fr.poubone.att2.client.util.ChatItemUtils;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ItemShareCodecTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void sharedChatKeepsCustomNameColorAndNativeTooltipAfterRoundTrip() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Lame d'Éolorion")
                .withStyle(s -> s.withColor(0xF472B6).withItalic(false)));
        original.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Dégâts : +42").withColor(0x55FF55))));
        original.set(DataComponents.TOOLTIP_STYLE, Identifier.fromNamespaceAndPath("att2", "special"));
        original.setDamageValue(17);
        String encoded = ItemShareCodec.encode(original, registries).orElseThrow();
        ItemStack received = ItemShareCodec.decode(encoded, registries);
        assertTrue(ItemStack.matches(original, received));
        Component message = ChatItemUtils.message(received, "Alex a partagé");
        assertEquals("Alex a partagé [Lame d'Éolorion]", message.getString());
        message.visit((style, text) -> {
            if (text.contains("Lame")) {
                assertEquals(0xF472B6, style.getColor().getValue());
                assertFalse(style.isItalic());
                HoverEvent.ShowItem hover = assertInstanceOf(HoverEvent.ShowItem.class, style.getHoverEvent());
                assertTrue(ItemStack.matches(original, hover.item()));
                assertEquals(original.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL),
                        hover.item().getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL));
                assertEquals(original.get(DataComponents.TOOLTIP_STYLE), hover.item().get(DataComponents.TOOLTIP_STYLE));
            }
            return Optional.empty();
        }, Style.EMPTY);
        received.set(DataComponents.CUSTOM_NAME, Component.literal("Changed afterwards"));
        assertTrue(message.getString().contains("Lame d'Éolorion"));
    }

    @Test void invalidOrOversizedPeerDataIsIgnored() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        assertTrue(ItemShareCodec.decode("{broken", registries).isEmpty());
        assertTrue(ItemShareCodec.decode("x".repeat(ItemShareCodec.MAX_SNBT_CHARS + 1), registries).isEmpty());
    }
}
