package fr.poubone.att2.client.quest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestBookLayoutTest {
    @Test void bothPagesAndControlsFitAllSupportedGuiSizesWithoutShrinkingText() {
        for (int[] size : new int[][]{{320,180},{320,240},{426,240},{480,270},{640,360},{960,540},{1280,720}}) {
            var l = QuestBookLayout.fit(size[0],size[1]);
            String at = size[0] + "x" + size[1];
            assertTrue(l.x() >= 0 && l.y() >= 0, at);
            assertTrue(l.x() + l.width() <= size[0] && l.y() + l.height() <= size[1], at);
            assertTrue(l.contentWidth() >= 110, at);
            assertTrue(l.leftX() + l.contentWidth() < l.rightX(), at);
            assertTrue(l.rightX() + l.contentWidth() < l.x() + l.width(), at);
            assertTrue(l.textScale() >= 1f, at);
            assertTrue(l.rowsPerPage() >= 1, at);
            assertTrue(l.listTop() + l.rowsPerPage() * l.rowHeight() <= l.footerY() - 8, at);
            assertTrue(l.detailBottom() - l.detailTop() >= 50, at);
            assertTrue(l.filterY() + (4/l.filterColumns())*22 < l.listTop(), at);
            assertTrue(l.footerY() + 20 < l.y() + l.height(), at);
        }
    }

    @Test void smallWindowsReduceRowsInsteadOfFontSize() {
        var compact = QuestBookLayout.fit(320,180);
        var large = QuestBookLayout.fit(640,360);
        assertEquals(1, compact.rowsPerPage());
        assertTrue(large.rowsPerPage() > compact.rowsPerPage());
        assertEquals(1f, compact.textScale());
        assertTrue(large.textScale() > 1f);
    }
}
