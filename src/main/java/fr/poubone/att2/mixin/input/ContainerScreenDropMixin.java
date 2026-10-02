package fr.poubone.att2.mixin.input;

import fr.poubone.att2.client.input.DropLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercepts inventory-screen drops (throw key on a slot, click outside with a carried item). */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenDropMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void att2$confirmScreenDrop(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (type == ClickType.THROW && slot != null && slot.hasItem()
                && DropLock.shouldBlock(client, slot.getItem())) {
            ci.cancel();
            return;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (slotId == -999 && type == ClickType.PICKUP
                && DropLock.shouldBlock(client, self.getMenu().getCarried())) {
            ci.cancel();
        }
    }
}
