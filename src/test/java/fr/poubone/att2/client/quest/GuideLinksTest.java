package fr.poubone.att2.client.quest;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuideLinksTest {
    @Test void sideQuestOpensItsBookOnTheFrenchLibrary() {
        assertEquals(URI.create("https://guide-att2.com/#rayon=side&livre=sq01"),
                GuideLinks.page(QuestType.SIDE, 1, null, "fr"));
        assertEquals(URI.create("https://guide-att2.com/#rayon=side&livre=sq60"),
                GuideLinks.page(QuestType.SIDE, 60, null, "fr"));
    }

    @Test void otherLanguagesUseTheSitePrefix() {
        assertEquals(URI.create("https://guide-att2.com/en/#rayon=side&livre=sq07"),
                GuideLinks.page(QuestType.SIDE, 7, null, "en"));
        assertEquals(URI.create("https://guide-att2.com/zh/#rayon=side&livre=sq07"),
                GuideLinks.page(QuestType.SIDE, 7, null, "zh"));
        assertEquals(URI.create("https://guide-att2.com/#rayon=side&livre=sq07"),
                GuideLinks.page(QuestType.SIDE, 7, null, "zz"));
    }

    @Test void sideQuestOutsideTheGuideReturnsNothing() {
        assertNull(GuideLinks.page(QuestType.SIDE, 0, null, "fr"));
        assertNull(GuideLinks.page(QuestType.SIDE, 61, null, "fr"));
    }

    @Test void mainQuestFollowsTheActBooks() {
        assertEquals("#rayon=main", GuideLinks.hash(QuestType.MAIN, -1, null));
        assertEquals("#rayon=main&livre=acte-1", GuideLinks.hash(QuestType.MAIN, 0, null));
        assertEquals("#rayon=main&livre=acte-2", GuideLinks.hash(QuestType.MAIN, 1, null));
        assertEquals("#rayon=main&livre=acte-2", GuideLinks.hash(QuestType.MAIN, 50, null));
        assertEquals("#rayon=main&livre=acte-3", GuideLinks.hash(QuestType.MAIN, 51, null));
        assertEquals("#rayon=main&livre=acte-3", GuideLinks.hash(QuestType.MAIN, 90, null));
        assertEquals("#rayon=main&livre=acte-4", GuideLinks.hash(QuestType.MAIN, 91, null));
        assertEquals("#rayon=main&livre=acte-4", GuideLinks.hash(QuestType.MAIN, 280, null));
        assertEquals("#rayon=main&livre=acte-5", GuideLinks.hash(QuestType.MAIN, 281, null));
        assertEquals("#rayon=main&livre=acte-5", GuideLinks.hash(QuestType.MAIN, 300, null));
    }

    @Test void dailyQuestUsesTheCitySlug() {
        assertEquals(URI.create("https://guide-att2.com/#rayon=daily&livre=dq-meleim-2"),
                GuideLinks.page(QuestType.DAILY, 2, "meleim", "fr"));
        assertEquals(URI.create("https://guide-att2.com/de/#rayon=daily&livre=dq-eolorion-9"),
                GuideLinks.page(QuestType.DAILY, 9, "Eolorion", "de"));
        assertEquals(URI.create("https://guide-att2.com/#rayon=daily&livre=dq-meleim-1"),
                GuideLinks.page(QuestType.DAILY, 1, "Méleïm", "fr"));
    }

    @Test void dailyQuestWithoutACityReturnsNothing() {
        assertNull(GuideLinks.page(QuestType.DAILY, 1, null, "fr"));
        assertNull(GuideLinks.page(QuestType.DAILY, 1, "   ", "fr"));
        assertNull(GuideLinks.page(QuestType.DAILY, 0, "ryliath", "fr"));
    }

    @Test void everyLinkStaysOnTheGuideHost() {
        for (QuestType type : QuestType.values()) {
            URI uri = GuideLinks.page(type, 12, "phoenix", "en");
            if (uri != null) assertEquals("guide-att2.com", uri.getHost());
        }
    }
}
