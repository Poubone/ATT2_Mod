package fr.poubone.att2.client.teleport;

import fr.poubone.att2.client.compat.FlashbackCompat;
import net.minecraft.client.Minecraft;

public final class TeleportCommandSender {
    private static boolean bypassNextCommand;

    private TeleportCommandSender() {
    }

    public static boolean isBypassing() {
        return bypassNextCommand;
    }

    public static void sendDeferredCommand(String command) {
        if (FlashbackCompat.isInReplay()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return;
        }

        bypassNextCommand = true;
        try {
            client.getConnection().sendCommand(command);
        } finally {
            bypassNextCommand = false;
        }
    }
}
