package fr.poubone.att2.mixin;

import fr.poubone.att2.client.util.ModLanguageManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.shop.ShopComparison;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** The shop's equipment comparison, on items hovered in the player's own inventory screen. */
@Mixin(AbstractContainerScreen.class)
public abstract class InventoryComparisonMixin extends Screen {
    @Shadow protected Slot hoveredSlot;
    @Shadow @Final protected AbstractContainerMenu menu;

    @Shadow protected abstract List<Component> getTooltipFromContainerItem(ItemStack stack);

    protected InventoryComparisonMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void att2$compareHoveredItem(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        ItemStack stack = comparableHoveredStack();
        if (stack == null || !compareHeld()) return;
        ShopComparison.render(graphics, getTooltipFromContainerItem(stack), Minecraft.getInstance().player,
                ShopComparison.kind(stack), stack, mouseX, mouseY, width, height);
        ci.cancel();
    }

    @ModifyReturnValue(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private List<Component> att2$addCompareHint(List<Component> lines, ItemStack stack) {
        if (stack != comparableHoveredStack() || compareHeld()) return lines;
        List<Component> withHint = new ArrayList<>(lines);
        withHint.add(ModLanguageManager.shared("att2.ui.compare", "key", KeybindManager.compareKeyLabel())
                .withStyle(ChatFormatting.GRAY));
        return withHint;
    }

    /** The hovered stack when this is the inventory screen, nothing is carried, and it has something to compare with. */
    private ItemStack comparableHoveredStack() {
        var player = Minecraft.getInstance().player;
        if (!((Object) this instanceof InventoryScreen) || player == null) return null;
        if (hoveredSlot == null || !hoveredSlot.hasItem() || !menu.getCarried().isEmpty()) return null;
        ItemStack stack = hoveredSlot.getItem();
        return ShopComparison.hasComparison(player, stack) ? stack : null;
    }

    private static boolean compareHeld() {
        return ShopComparison.isHeld(Minecraft.getInstance().getWindow().handle(), KeybindManager.compareKeyCode());
    }
}
