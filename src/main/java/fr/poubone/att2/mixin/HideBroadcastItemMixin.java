package fr.poubone.att2.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.poubone.att2.client.util.BroadcastTagged;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class HideBroadcastItemMixin {
	@Inject(
		method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void att2$hideBroadcastItem(
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int light,
		ArmedEntityRenderState state,
		float limbSwing,
		float limbSwingAmount,
		CallbackInfo ci
	) {
		if (state instanceof ArmorStandRenderState armorStandState
			&& ((BroadcastTagged) (Object) armorStandState).att2$isBroadcastStand()) {
			ci.cancel();
		}
	}
}
