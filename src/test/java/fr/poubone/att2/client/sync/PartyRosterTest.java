package fr.poubone.att2.client.sync;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartyRosterTest {

    @Test
    void alwaysIncludesSelfFirstEvenWhenTabIsEmpty() {
        UUID self = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals(List.of(self.toString()), PartyRoster.listedIds(self, List.of()));
    }

    @Test
    void deduplicatesSelfWhenAlreadyOnTheTabList() {
        UUID self = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID mate = UUID.fromString("00000000-0000-0000-0000-000000000002");
        List<String> ids = PartyRoster.listedIds(self, List.of(self, mate, mate));
        assertEquals(List.of(self.toString(), mate.toString()), ids);
    }
}
