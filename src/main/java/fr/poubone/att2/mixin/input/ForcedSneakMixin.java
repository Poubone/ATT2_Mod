package fr.poubone.att2.mixin.input;

import fr.poubone.att2.client.input.InputSequencer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forces the sneak flag while an InputSequencer step needs it (works with toggle-sneak too). */
@Mixin(KeyboardInput.class)
public abstract class ForcedSneakMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void att2$forceSneak(CallbackInfo ci) {
        if (InputSequencer.isSneakForced()) {
            this.keyPresses = new Input(
                    this.keyPresses.forward(), this.keyPresses.backward(),
                    this.keyPresses.left(), this.keyPresses.right(),
                    this.keyPresses.jump(), true, this.keyPresses.sprint());
        }
    }
}
