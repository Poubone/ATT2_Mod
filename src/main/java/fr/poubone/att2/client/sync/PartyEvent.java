package fr.poubone.att2.client.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** One room event returned by {@code POST /v1/heartbeat}. */
public record PartyEvent(int id, String type, String playerId, String playerName, JsonObject payload, long ts) {
    public static PartyEvent fromJson(JsonObject json) {
        JsonObject payload = new JsonObject();
        JsonElement raw = json.get("payload");
        if (raw != null && raw.isJsonObject()) {
            payload = raw.getAsJsonObject();
        }
        return new PartyEvent(
                json.has("id") ? json.get("id").getAsInt() : 0,
                json.has("type") ? json.get("type").getAsString() : "",
                json.has("playerId") ? json.get("playerId").getAsString() : "",
                json.has("playerName") ? json.get("playerName").getAsString() : "",
                payload,
                json.has("ts") ? json.get("ts").getAsLong() : 0L
        );
    }

    public boolean isPing() {
        return "ping".equals(type);
    }

    public boolean isItemShare() {
        return "item_share".equals(type);
    }
}
