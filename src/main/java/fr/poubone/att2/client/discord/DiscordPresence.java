package fr.poubone.att2.client.discord;

import fr.poubone.att2.client.compat.FlashbackCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestInfo;
import fr.poubone.att2.client.quest.QuestModel;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pushes a Discord Rich Presence activity while the player is in a world. Runs the IPC client
 * on a daemon thread so a missing Discord install never stalls the game.
 */
public final class DiscordPresence {
    private static final long UPDATE_INTERVAL_MS = 15_000;
    private static final int STATE_MAX = 128;
    private static final int LABEL_MAX = 32;

    private static final ConcurrentLinkedQueue<JsonObject> OUT = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean WORKER = new AtomicBoolean(false);

    private static volatile boolean wanted;
    private static volatile String lastFingerprint = "";
    private static volatile long sessionStartEpoch;
    private static long lastPushMs;
    private static boolean warnedMissingId;

    private DiscordPresence() {
    }

    public static void onJoin() {
        sessionStartEpoch = System.currentTimeMillis() / 1000L;
        lastFingerprint = "";
        lastPushMs = 0;
        syncWorker();
        tick();
    }

    public static void onDisconnect() {
        enqueueClear();
        wanted = false;
        lastFingerprint = "";
    }

    public static void onConfigChanged() {
        lastFingerprint = "";
        lastPushMs = 0;
        if (Minecraft.getInstance().player != null) {
            syncWorker();
            tick();
        } else {
            enqueueClear();
            wanted = false;
        }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        syncWorker();
        if (!wanted) return;

        String fingerprint = buildFingerprint();
        long now = System.currentTimeMillis();
        if (fingerprint.equals(lastFingerprint) && now - lastPushMs < UPDATE_INTERVAL_MS) return;
        lastFingerprint = fingerprint;
        lastPushMs = now;
        OUT.offer(setActivity(buildActivity()));
    }

    private static void syncWorker() {
        HUDConfig config = HUDConfig.get();
        boolean enable = !FlashbackCompat.isInReplay() && config.discordRichPresence
                && config.discordApplicationId != null
                && !config.discordApplicationId.isBlank();
        if (!enable) {
            if (wanted) {
                OUT.clear();
                enqueueClear();
                lastFingerprint = "";
                lastPushMs = 0;
            }
            if (!warnedMissingId && config.discordRichPresence
                    && (config.discordApplicationId == null || config.discordApplicationId.isBlank())) {
                warnedMissingId = true;
                System.err.println("[ATT2] Discord Rich Presence is on, but discordApplicationId is empty in config/att2_hud.json");
            }
            wanted = false;
            return;
        }
        wanted = true;
        startWorker(config.discordApplicationId.trim());
    }

    private static void startWorker(String clientId) {
        if (!WORKER.compareAndSet(false, true)) return;
        Thread thread = new Thread(() -> runWorker(clientId), "att2-discord-ipc");
        thread.setDaemon(true);
        thread.start();
    }

    private static void runWorker(String clientId) {
        long backoffMs = 2000;
        try {
            while (wanted) {
                DiscordIpcClient client = null;
                try {
                    client = DiscordIpcClient.connect(clientId);
                    backoffMs = 2000;
                    lastFingerprint = "";
                    while (wanted) {
                        JsonObject outgoing = OUT.poll();
                        if (outgoing != null) {
                            client.sendFrame(FlashbackCompat.isInReplay() ? setActivity(null) : outgoing);
                        }
                        JsonObject reply = client.poll();
                        // Discord answers SET_ACTIVITY with "evt": null; only log real ERROR events.
                        if (reply != null && reply.has("evt") && !reply.get("evt").isJsonNull()
                                && "ERROR".equals(reply.get("evt").getAsString())) {
                            System.err.println("[ATT2] Discord Rich Presence rejected: " + reply);
                        }
                        if (outgoing == null) {
                            Thread.sleep(50);
                        }
                    }
                    client.sendFrame(setActivity(null));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException e) {
                    if (wanted) {
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        backoffMs = Math.min(30_000, backoffMs * 2);
                    }
                } finally {
                    if (client != null) client.close();
                }
            }
        } finally {
            WORKER.set(false);
            OUT.clear();
        }
    }

    private static void enqueueClear() {
        OUT.offer(setActivity(null));
    }

    private static String buildFingerprint() {
        HUDConfig config = HUDConfig.get();
        return config.discordShowQuest + "|" + config.discordRegionImage + "|" + details() + "|" + state()
                + "|" + largeImageKey() + "|" + largeImageText()
                + "|" + config.discordMapUrl + "|" + config.discordModUrl + "|" + config.discordLargeImage;
    }

    private static JsonObject buildActivity() {
        HUDConfig config = HUDConfig.get();
        JsonObject activity = new JsonObject();
        activity.addProperty("type", 0);
        activity.addProperty("details", details());
        activity.addProperty("state", state());
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", sessionStartEpoch);
        activity.add("timestamps", timestamps);
        String image = largeImageKey();
        if (image != null && !image.isBlank()) {
            JsonObject assets = new JsonObject();
            assets.addProperty("large_image", image);
            assets.addProperty("large_text", largeImageText());
            activity.add("assets", assets);
        }
        // Order matches the design: mod first, map second (max 2 Discord buttons).
        JsonArray buttons = new JsonArray();
        addButton(buttons, ModLanguageManager.getString("discord.button.mod"), config.discordModUrl);
        addButton(buttons, ModLanguageManager.getString("discord.button.map"), config.discordMapUrl);
        if (buttons.size() > 0) {
            activity.add("buttons", buttons);
        }
        return activity;
    }

    private static String largeImageKey() {
        HUDConfig config = HUDConfig.get();
        String fallback = config.discordLargeImage == null || config.discordLargeImage.isBlank()
                ? DiscordRegion.FALLBACK_ASSET
                : config.discordLargeImage.trim();
        if (!config.discordRegionImage) return fallback;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return fallback;
        String regionKey = DiscordRegion.of(client.player).assetKey();
        return assetUploaded(config, regionKey) ? regionKey : fallback;
    }

    private static boolean assetUploaded(HUDConfig config, String key) {
        List<String> uploaded = config.discordUploadedAssets;
        if (uploaded == null || uploaded.isEmpty() || key == null) return false;
        for (String item : uploaded) {
            if (item == null) continue;
            String trimmed = item.trim();
            if (trimmed.equals("*") || trimmed.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private static String largeImageText() {
        HUDConfig config = HUDConfig.get();
        Minecraft client = Minecraft.getInstance();
        if (config.discordRegionImage && client.player != null) {
            String name = ModLanguageManager.getString(DiscordRegion.of(client.player).nameKey());
            if (name != null && !name.isBlank() && !name.startsWith("§c?")) return clip(name, STATE_MAX);
        }
        return details();
    }

    private static String details() {
        return clip(ModLanguageManager.getString("discord.details"), STATE_MAX);
    }

    private static String state() {
        HUDConfig config = HUDConfig.get();
        if (config.discordShowQuest) {
            QuestInfo selected = QuestModel.get().findByKey(QuestModel.get().getSelectedKey());
            if (selected != null) {
                String name = selected.plainName();
                if (name != null && !name.isBlank()) return clip(name, STATE_MAX);
            }
        }
        return clip(ModLanguageManager.getString("discord.exploring"), STATE_MAX);
    }

    private static void addButton(JsonArray buttons, String label, String url) {
        if (buttons.size() >= 2) return;
        if (label == null || label.isBlank() || label.startsWith("\u00a7c?")) return;
        if (url == null || url.isBlank()) return;
        String trimmed = url.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return;
        JsonObject button = new JsonObject();
        button.addProperty("label", clip(label, LABEL_MAX));
        button.addProperty("url", trimmed);
        buttons.add(button);
    }

    private static JsonObject setActivity(JsonObject activity) {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        if (activity == null) {
            args.add("activity", JsonNull.INSTANCE);
        } else {
            args.add("activity", activity);
        }
        JsonObject cmd = new JsonObject();
        cmd.addProperty("nonce", UUID.randomUUID().toString());
        cmd.addProperty("cmd", "SET_ACTIVITY");
        cmd.add("args", args);
        return cmd;
    }

    private static String clip(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }

}
