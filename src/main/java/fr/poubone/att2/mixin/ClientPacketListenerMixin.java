package fr.poubone.att2.mixin;

import fr.poubone.att2.client.gambling.GamblingModel;
import fr.poubone.att2.client.hud.CityToast;
import fr.poubone.att2.client.data.SpellXpRefresh;
import fr.poubone.att2.client.renderer.ItemParticleBudget;
import fr.poubone.att2.client.teleport.TeleportCommandSender;
import fr.poubone.att2.client.teleport.WaypointTeleportDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes outgoing commands and incoming sounds to detect the map's teleporters. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleOpenBook", at = @At("HEAD"), cancellable = true)
    private void att2$keepSpellRadialOpen(ClientboundOpenBookPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread() && SpellXpRefresh.suppressBookScreen()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void att2$observeCommand(String command, CallbackInfo ci) {
        if (!TeleportCommandSender.isBypassing()) {
            WaypointTeleportDetector.onOutgoingCommand(command);
        }
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), require = 0)
    private void att2$observeUnattendedCommand(String command, Screen screen, CallbackInfo ci) {
        if (!TeleportCommandSender.isBypassing()) {
            WaypointTeleportDetector.onOutgoingCommand(command);
        }
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void att2$observeSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        // The handler is first called on the network thread and re-dispatched to the render thread.
        if (!Minecraft.getInstance().isSameThread()) return;
        WaypointTeleportDetector.onSoundPacket(
                packet.getSound().value().location(),
                packet.getSource(),
                packet.getX(), packet.getY(), packet.getZ());
        GamblingModel.get().onSound(packet.getSound().value().location(), packet.getSource());
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void att2$thinItemParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        switch (ItemParticleBudget.classify(packet)) {
            case HIDE -> ci.cancel();
            case SHOW -> ItemParticleBudget.setSpawningItemDust(true);
            case NOT_ITEM_DUST -> {
            }
        }
    }

    @Inject(method = "handleParticleEvent", at = @At("RETURN"))
    private void att2$endItemParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        ItemParticleBudget.setSpawningItemDust(false);
    }

    @Inject(method = "setTitleText", at = @At("HEAD"), cancellable = true)
    private void att2$observeTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        GamblingModel.get().onTitle(packet.text());
        if (CityToast.consumeTitle(packet.text())) {
            ci.cancel();
        }
    }

    @Inject(method = "setSubtitleText", at = @At("HEAD"), cancellable = true)
    private void att2$observeSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (CityToast.consumeSubtitle(packet.text())) {
            ci.cancel();
        }
    }
}
