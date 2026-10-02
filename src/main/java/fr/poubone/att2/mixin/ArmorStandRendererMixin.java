package fr.poubone.att2.mixin;

import fr.poubone.att2.client.util.BroadcastTagged;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandRenderer.class)
public class ArmorStandRendererMixin {
	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V",
		at = @At("TAIL")
	)
	private void att2$markBroadcastStand(ArmorStand entity, ArmorStandRenderState state, float partialTick, CallbackInfo ci) {
		BroadcastTagged tagged = (BroadcastTagged) (Object) state;
		tagged.att2$setBroadcastStand(
			entity.getCustomName() != null && "[BROADCAST_TAG]".equals(entity.getCustomName().getString())
		);
	}
}
