package fr.poubone.att2.mixin;

import fr.poubone.att2.client.util.BroadcastTagged;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ArmorStandRenderState.class)
public class ArmorStandRenderStateMixin implements BroadcastTagged {
	@Unique
	private boolean att2$broadcastStand;

	@Override
	public boolean att2$isBroadcastStand() {
		return this.att2$broadcastStand;
	}

	@Override
	public void att2$setBroadcastStand(boolean broadcastStand) {
		this.att2$broadcastStand = broadcastStand;
	}
}
