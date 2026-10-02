package fr.poubone.att2.client.sync;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.poubone.att2.client.hud.HUDConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Blocking HTTP calls to the party-sync VPS. Always run off the client thread. */
public final class PartySyncHttp {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static final class ApiException extends IllegalStateException {
        public final int status;
        public final long retryAfterMs;
        ApiException(int status, long retryAfterMs) {
            super("HTTP " + status);
            this.status = status;
            this.retryAfterMs = retryAfterMs;
        }
    }

    static long retryDelay(String value) {
        try { return Math.max(1, Math.min(5, Long.parseLong(value))) * 1_000; }
        catch (NumberFormatException ignored) { return 1_000; }
    }

    private PartySyncHttp() {
    }

    public static JsonObject heartbeat(String playerId, String playerName, List<String> seenIds, int sinceEventId)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("playerId", playerId);
        body.addProperty("playerName", playerName);
        JsonArray seen = new JsonArray();
        for (String id : seenIds) {
            seen.add(id);
        }
        body.add("seenPlayerIds", seen);
        body.addProperty("sinceEventId", sinceEventId);
        return post("/v1/heartbeat", body);
    }

    public static void publish(String playerId, String type, JsonObject payload) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("playerId", playerId);
        body.addProperty("type", type);
        body.add("payload", payload);
        post("/v1/event", body);
    }

    public static void leave(String playerId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("playerId", playerId);
            post("/v1/leave", body);
        } catch (Exception ignored) {
        }
    }

    public static List<PartyEvent> eventsOf(JsonObject response) {
        List<PartyEvent> events = new ArrayList<>();
        if (response == null || !response.has("events") || !response.get("events").isJsonArray()) {
            return events;
        }
        for (JsonElement element : response.getAsJsonArray("events")) {
            if (element.isJsonObject()) {
                try {
                    events.add(PartyEvent.fromJson(element.getAsJsonObject()));
                } catch (IllegalArgumentException | IllegalStateException ignored) {
                    // Keep valid events when a peer sends one malformed entry.
                }
            }
        }
        return events;
    }

    private static JsonObject post(String path, JsonObject body) throws Exception {
        String base = configuredBase();
        if (base == null) {
            throw new IllegalStateException("party sync url is empty");
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(4))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new ApiException(status, retryDelay(response.headers().firstValue("Retry-After").orElse("1")));
        }
        String raw = response.body();
        if (raw == null || raw.isBlank()) {
            return new JsonObject();
        }
        return GSON.fromJson(raw, JsonObject.class);
    }

    public static String configuredBase() {
        String url = HUDConfig.get().partySyncUrl;
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static boolean isConfigured() {
        return configuredBase() != null;
    }
}
