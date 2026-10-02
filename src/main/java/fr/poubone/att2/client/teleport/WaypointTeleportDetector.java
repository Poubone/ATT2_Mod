package fr.poubone.att2.client.teleport;

import fr.poubone.att2.client.data.Att2Triggers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects the map's town teleporters ("Teleportation Arrays", {@code gameplay/waypoint/*}) purely client-side
 * and starts the Grand Teleport transition around them.
 * <p>
 * Flow in the datapack: the player clicks a destination in chat, which runs {@code /trigger ScoreTrigger set N}
 * (N = 576..617, one per origin/destination pair). {@code waypoint/start} then plays {@code minecraft:noise1}
 * and counts a 100-tick timer down; the actual {@code teleport} happens when the timer reaches 9, i.e.
 * {@value #TP_DELAY_TICKS} ticks after the sound.
 * <p>
 * We therefore learn the destination from the outgoing command, and use the sound as the confirmation that the
 * teleport really started (the command may be refused: wrong position, daily quest lock...). If the sound comes
 * without a known command (dialog/command block), the animation runs with the destination discovered on arrival.
 */
public final class WaypointTeleportDetector {
    /** Ticks between {@code noise1} and the datapack's {@code teleport}. */
    public static final int TP_DELAY_TICKS = 91;
    private static final int COMMAND_CONFIRM_TIMEOUT_TICKS = 40;
    private static final double PAD_RADIUS = 8.0D;
    private static final int FIRST_TRIGGER = 576;
    private static final Identifier START_SOUND = Identifier.withDefaultNamespace("noise1");

    public record Pad(String name, Vec3 pos) {
    }

    public record Route(Pad from, Pad to) {
    }

    /** Alphabetical order matters: it is the order used by the datapack's trigger numbering. */
    public static final List<Pad> PADS = List.of(
            new Pad("asunark", new Vec3(-3661, 70, -4977)),
            new Pad("eolorion", new Vec3(-5248, 100, -6202)),
            new Pad("kortaek", new Vec3(-5533, 87, -4678)),
            new Pad("meleim", new Vec3(-3845, 103, -5808)),
            new Pad("owsastr", new Vec3(-4661, 71, -4546)),
            new Pad("ryliath", new Vec3(-5005, 77, -5077)),
            new Pad("soquai", new Vec3(-4790, 100, -5686))
    );

    private static final Map<Integer, Route> ROUTES = new HashMap<>();

    static {
        int trigger = FIRST_TRIGGER;
        for (Pad from : PADS) {
            for (Pad to : PADS) {
                if (from == to) continue;
                ROUTES.put(trigger++, new Route(from, to));
            }
        }
    }

    private static Route pendingRoute;
    private static int pendingTicks;

    private WaypointTeleportDetector() {
    }

    public static void reset() {
        pendingRoute = null;
        pendingTicks = 0;
    }

    public static Route routeForTrigger(int trigger) {
        return ROUTES.get(trigger);
    }

    /** Called for every command the client sends. Never cancels the command. */
    public static void onOutgoingCommand(String command) {
        if (!TeleportAnimationConfig.isEffectEnabled()) return;
        int trigger = Att2Triggers.parseTriggerCommand(command);
        Route route = ROUTES.get(trigger);
        if (route == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isNear(player.position(), route.from().pos(), PAD_RADIUS)) return;

        pendingRoute = route;
        pendingTicks = COMMAND_CONFIRM_TIMEOUT_TICKS;
    }

    /** Called for every positional sound packet received. */
    public static void onSoundPacket(Identifier sound, SoundSource source, double x, double y, double z) {
        if (!TeleportAnimationConfig.isEffectEnabled() || !START_SOUND.equals(sound) || source != SoundSource.BLOCKS) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;
        if (!isNear(player.position(), new Vec3(x, y, z), 3.0D)) return;
        if (TeleportTransitionController.isRunning()) return;

        Vec3 target = null;
        if (pendingRoute != null) {
            target = pendingRoute.to().pos();
        } else if (nearestPad(player.position()) == null) {
            return;
        }
        pendingRoute = null;
        pendingTicks = 0;

        TeleportTransitionController.startExternal(client, target, TP_DELAY_TICKS);
    }

    public static void tick(Minecraft client) {
        if (pendingTicks > 0) {
            pendingTicks--;
            if (pendingTicks == 0) pendingRoute = null;
        }
    }

    private static Pad nearestPad(Vec3 pos) {
        for (Pad pad : PADS) {
            if (isNear(pos, pad.pos(), PAD_RADIUS)) return pad;
        }
        return null;
    }

    private static boolean isNear(Vec3 a, Vec3 b, double radius) {
        return a.distanceToSqr(b) <= radius * radius;
    }
}
