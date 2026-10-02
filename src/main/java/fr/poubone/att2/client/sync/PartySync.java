package fr.poubone.att2.client.sync;

import fr.poubone.att2.client.compat.FlashbackCompat;
import com.google.gson.JsonObject;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.util.ChatItemUtils;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client loop: TAB heartbeat every second, apply item shares and pings on the game thread.
 */
public final class PartySync {
    private static final long HEARTBEAT_MS = 1_000;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "att2-party-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final ConcurrentLinkedQueue<PartyEvent> INCOMING = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean HEARTBEAT_IN_FLIGHT = new AtomicBoolean(false);

    private static final AtomicBoolean PING_IN_FLIGHT = new AtomicBoolean(false);
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("att2-party-sync");
    private static volatile long nextHeartbeatMs;
    private static volatile long lastWarningMs;
    private static volatile int lastEventId;
    private static volatile UUID sessionPlayer;
    private static long lastHeartbeatMs;
    private static volatile boolean warnedUnreachable;
    private static long pingSequence;
    private static long lastPingMs;
    private static volatile long sessionGeneration;
    private static boolean replaySuspended;

    private PartySync() {
    }

    public static void tick(Minecraft client) {
        boolean replay = FlashbackCompat.isInReplay();
        if (replay != replaySuspended) {
            replaySuspended = replay;
            if (replay) onDisconnect();
            else onJoin();
        }
        if (!isEnabled()) {
            if (sessionPlayer != null) onDisconnect();
            return;
        }
        drainIncoming(client);
        PingMarkers.tick(client);
        if (client.player == null || client.level == null || !PartySyncHttp.isConfigured()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextHeartbeatMs || now - lastHeartbeatMs < HEARTBEAT_MS || HEARTBEAT_IN_FLIGHT.get()) {
            return;
        }
        lastHeartbeatMs = now;
        UUID self = client.player.getUUID();
        sessionPlayer = self;
        String name = client.player.getName().getString();
        List<String> seen = tabIds(client, self);
        long generation = sessionGeneration;
        HEARTBEAT_IN_FLIGHT.set(true);
        IO.execute(() -> {
            try {
                if (generation != sessionGeneration || !isEnabled()) return;
                JsonObject response = PartySyncHttp.heartbeat(self.toString(), name, seen, lastEventId);
                ingestHeartbeat(response, generation);
                warnedUnreachable = false;
            } catch (Exception failure) {
                if (generation == sessionGeneration && isEnabled()) {
                    nextHeartbeatMs = System.currentTimeMillis() + 5_000;
                    warnUnreachable(failure);
                }
            } finally {
                HEARTBEAT_IN_FLIGHT.set(false);
            }
        });
    }

    public static synchronized void onJoin() {
        sessionGeneration++;
        lastEventId = 0;
        lastHeartbeatMs = 0;
        nextHeartbeatMs = 0;
        lastWarningMs = 0;
        INCOMING.clear();
        PingMarkers.clear();
        warnedUnreachable = false;
        lastPingMs = 0;
    }

    public static synchronized void onDisconnect() {
        sessionGeneration++;
        UUID id = sessionPlayer;
        INCOMING.clear();
        PingMarkers.clear();
        lastEventId = 0;
        sessionPlayer = null;
        if (id != null && PartySyncHttp.isConfigured()) {
            IO.execute(() -> PartySyncHttp.leave(id.toString()));
        }
    }

    public static boolean isEnabled() {
        return !FlashbackCompat.isInReplay()
                && HUDConfig.get().partySyncEnabled && PartySyncHttp.isConfigured();
    }

    public static void onConfigChanged() {
        onDisconnect();
        if (isEnabled()) onJoin();
    }

    public static void shareHeldItem(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null) {
            return;
        }
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            client.player.displayClientMessage(
                    Component.literal("\u00a7c" + ModLanguageManager.getString("keybind.broadcast.empty")), true);
            return;
        }
        var encoded = ItemShareCodec.encode(stack, client.level.registryAccess());
        if (encoded.isEmpty()) {
            client.player.displayClientMessage(ModLanguageManager.get("keybind.broadcast.too_large"), true);
            return;
        }
        String playerName = client.player.getName().getString();
        ChatItemUtils.sendItemInChat(stack, playerName + " " + ModLanguageManager.getString("keybind.broadcast.shared"));
        JsonObject payload = new JsonObject();
        payload.addProperty("itemName", ItemShareCodec.displayName(stack));
        payload.addProperty("itemId", ItemShareCodec.itemId(stack));
        payload.addProperty("itemSnbt", encoded.get());
        UUID self = client.player.getUUID();
        List<String> seen = tabIds(client, self);
        long generation = sessionGeneration;
        IO.execute(() -> publishOrHeartbeat(self.toString(), playerName, seen, "item_share", payload, generation));
    }

    public static void pingLookedAt(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null) {
            return;
        }
        if (PingMarkers.dismissLookedAt()) return;
        PingTargeting.Target target = PingTargeting.lookAt(client);
        if (target == null) {
            client.player.displayClientMessage(
                    Component.literal("\u00a7c" + ModLanguageManager.getString("keybind.ping.miss")), true);
            return;
        }
        Vec3 pos = target.pos();
        String dimension = String.valueOf(client.level.dimension());
        String playerName = client.player.getName().getString();
        long now = System.currentTimeMillis();
        if (now - lastPingMs < 250 || !PING_IN_FLIGHT.compareAndSet(false, true)) return;
        if (now - lastPingMs > 1_000) pingSequence++;
        lastPingMs = now;
        PingMarkers.add(client.player.getUUID(), playerName, pos, dimension,
                target.entityId(), target.item(), pingSequence, true);
        JsonObject payload = new JsonObject();
        payload.addProperty("x", pos.x);
        payload.addProperty("y", pos.y);
        payload.addProperty("z", pos.z);
        payload.addProperty("dimension", dimension);
        payload.addProperty("targetLabel", target.label());
        payload.addProperty("sequence", pingSequence);
        if (target.entityId() != null) payload.addProperty("entityId", target.entityId().toString());
        ItemShareCodec.encode(target.item(), client.level.registryAccess())
                .ifPresent(snbt -> payload.addProperty("itemSnbt", snbt));
        UUID self = client.player.getUUID();
        List<String> seen = tabIds(client, self);
        long generation = sessionGeneration;
        IO.execute(() -> {
            try { publishOrHeartbeat(self.toString(), playerName, seen, "ping", payload, generation); }
            finally { PING_IN_FLIGHT.set(false); }
        });
    }

    private static void publishOrHeartbeat(
            String playerId, String playerName, List<String> seen, String type, JsonObject payload, long generation) {
        if (generation != sessionGeneration || !isEnabled()) return;
        try {
            PartySyncRetry.publish(() -> {
                try {
                    PartySyncHttp.publish(playerId, type, payload);
                } catch (PartySyncHttp.ApiException ex) {
                    if (ex.status != 409) throw ex;
                    if (generation != sessionGeneration || !isEnabled()) return;
                    ingestHeartbeat(PartySyncHttp.heartbeat(playerId, playerName, seen, lastEventId), generation);
                    if (generation == sessionGeneration && isEnabled()) PartySyncHttp.publish(playerId, type, payload);
                }
            }, Thread::sleep, () -> generation == sessionGeneration && isEnabled());
            warnedUnreachable = false;
        } catch (Exception failure) {
            if (generation == sessionGeneration && isEnabled()) warnUnreachable(failure);
        }
    }

    /** Show one unobtrusive status message until the API succeeds again. */
    private static void warnUnreachable(Exception failure) {
        long now = System.currentTimeMillis();
        if (warnedUnreachable || now - lastWarningMs < 5_000) {
            return;
        }
        warnedUnreachable = true;
        lastWarningMs = now;
        LOGGER.warn("Party-sync request failed: {}", failure.toString());
        String key = failure instanceof PartySyncHttp.ApiException api && api.status == 429
                ? "party_sync.rate_limited" : failure instanceof PartySyncHttp.ApiException
                ? "party_sync.http_error" : "party_sync.unreachable";
        String detail = failure instanceof PartySyncHttp.ApiException api ? " (HTTP " + api.status + ")" : "";
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (isEnabled() && client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("\u00a7c" + ModLanguageManager.getString(key) + detail), true);
            }
        });
    }

    private static synchronized void ingestHeartbeat(JsonObject response, long generation) {
        if (generation != sessionGeneration || !isEnabled()) return;
        if (response.has("lastEventId")) {
            lastEventId = Math.max(lastEventId, response.get("lastEventId").getAsInt());
        }
        for (PartyEvent event : PartySyncHttp.eventsOf(response)) {
            INCOMING.offer(event);
            lastEventId = Math.max(lastEventId, event.id());
        }
    }

    private static void drainIncoming(Minecraft client) {
        PartyEvent event;
        while ((event = INCOMING.poll()) != null) {
            try {
                apply(client, event);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                // A malformed peer event must not interrupt the game loop or later events.
            }
        }
    }

    private static void apply(Minecraft client, PartyEvent event) {
        if (!isEnabled() || client.player == null || client.level == null) {
            return;
        }
        if (event.isItemShare()) {
            ItemStack stack = ItemShareCodec.decode(
                    string(event.payload(), "itemSnbt"),
                    client.level.registryAccess());
            String label = event.playerName() + " " + ModLanguageManager.getString("keybind.broadcast.shared");
            if (!stack.isEmpty()) {
                ChatItemUtils.sendItemInChat(stack, label);
            } else {
                String fallback = string(event.payload(), "itemName");
                client.player.displayClientMessage(
                        Component.literal(fallback.isEmpty() ? label : label + " " + fallback), false);
            }
            return;
        }
        if (event.isPing()) {
            JsonObject payload = event.payload();
            if (!payload.has("x") || !payload.has("y") || !payload.has("z")) {
                return;
            }
            Vec3 pos = new Vec3(payload.get("x").getAsDouble(), payload.get("y").getAsDouble(), payload.get("z").getAsDouble());
            String dimension = string(payload, "dimension");
            UUID author = UUID.fromString(event.playerId());
            String entity = string(payload, "entityId");
            UUID entityId = entity.isEmpty() ? null : UUID.fromString(entity);
            long sequence = payload.has("sequence") ? payload.get("sequence").getAsLong() : event.id();
            ItemStack item = ItemShareCodec.decode(string(payload, "itemSnbt"), client.level.registryAccess());
            PingMarkers.add(author, event.playerName(), pos, dimension, entityId, item, sequence, true);
        }
    }

    private static List<String> tabIds(Minecraft client, UUID self) {
        List<UUID> listed = new ArrayList<>();
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                if (info != null && info.getProfile() != null && info.getProfile().id() != null) {
                    listed.add(info.getProfile().id());
                }
            }
        }
        return PartyRoster.listedIds(self, listed);
    }

    private static String string(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return "";
        }
        try {
            return json.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
