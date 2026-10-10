package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;

/**
 * Whether merchant cards show the "click to buy" line. Until the player picks a setting it is shown, and it
 * switches itself off for good once the world's play-time statistic reaches {@link #AUTO_HIDE_TICKS}.
 */
public final class ShopBuyHint {
    /** Ten hours of play. */
    static final int AUTO_HIDE_TICKS = 10 * 60 * 60 * 20;

    private ShopBuyHint() {
    }

    /** The client only learns its statistics on request, as the vanilla Statistics screen does. */
    static void requestPlayTime(Minecraft client) {
        if (HUDConfig.get().shopBuyHint != null || client.getConnection() == null) return;
        client.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    public static boolean isShown(Minecraft client) {
        HUDConfig config = HUDConfig.get();
        if (config.shopBuyHint != null) return config.shopBuyHint;
        if (client.player != null
                && client.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) >= AUTO_HIDE_TICKS) {
            config.shopBuyHint = false;
            HUDConfig.save();
            return false;
        }
        return true;
    }
}
