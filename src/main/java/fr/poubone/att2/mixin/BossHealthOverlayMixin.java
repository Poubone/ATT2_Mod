package fr.poubone.att2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.poubone.att2.client.data.MapStatBar;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the map's stat bossbar into {@link MapStatBar} and hides it while the stats HUD is on.
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Shadow
    @Final
    Map<UUID, LerpingBossEvent> events;

    @Inject(method = "update", at = @At("RETURN"))
    private void att2$captureStatBar(ClientboundBossEventPacket packet, CallbackInfo ci) {
        MapStatBar.capture(events, true);
    }

    @WrapMethod(method = "render")
    private void att2$hideStatBar(GuiGraphics graphics, Operation<Void> original) {
        MapStatBar.capture(events, true);
        final Map<UUID, LerpingBossEvent> parked = new LinkedHashMap<>();
        if (KeybindManager.showCustomHUD && HUDConfig.get().showStats) {
            events.entrySet().removeIf(entry -> {
                if (!MapStatBar.isStatBar(entry.getValue())) return false;
                parked.put(entry.getKey(), entry.getValue());
                return true;
            });
        }
        try {
            original.call(graphics);
        } finally {
            if (!parked.isEmpty()) {
                events.putAll(parked);
            }
        }
    }
}
