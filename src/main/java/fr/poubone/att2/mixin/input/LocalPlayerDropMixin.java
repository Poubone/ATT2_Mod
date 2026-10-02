package fr.poubone.att2.mixin.input;

import fr.poubone.att2.client.input.DropLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intercepts the vanilla drop key (Q) before the item leaves the hand. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void att2$confirmDrop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (DropLock.shouldBlock(Minecraft.getInstance(), self.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }
}
