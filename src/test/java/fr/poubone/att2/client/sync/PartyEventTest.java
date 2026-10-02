package fr.poubone.att2.client.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyEventTest {

    @Test
    void parsesAPingPayload() {
        JsonObject json = JsonParser.parseString("""
                {"id":4,"type":"ping","playerId":"00000000-0000-0000-0000-000000000002",
                 "playerName":"Alex","payload":{"x":1.5,"y":64,"z":-8,"dimension":"minecraft:overworld"},"ts":10}
                """).getAsJsonObject();
        PartyEvent event = PartyEvent.fromJson(json);
        assertTrue(event.isPing());
        assertEquals("Alex", event.playerName());
        assertEquals(1.5, event.payload().get("x").getAsDouble());
        assertEquals(4, event.id());
    }
}
