package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopViewportTest {
    @Test void honorsGuiUnitsUntilThePanelNeedsToShrink() {
        var smallGui = ShopViewport.fit(1920, 1080, 640, 392);
        var mediumGui = ShopViewport.fit(960, 540, 640, 392);
        var largeGui = ShopViewport.fit(480, 270, 640, 392);
        assertEquals(1f, smallGui.scale());
        assertEquals(1f, mediumGui.scale());
        assertTrue(largeGui.scale() < 1f);
    }

    @Test void sizeScalesThePanelUntilItNeedsToShrink() {
        assertEquals(0.5f, ShopViewport.fit(1920, 1080, 640, 392, 50).scale());
        assertEquals(1.5f, ShopViewport.fit(1920, 1080, 640, 392, 150).scale());
        var tooBig = ShopViewport.fit(640, 360, 640, 392, 150);
        assertEquals(ShopViewport.fit(640, 360, 640, 392).scale(), tooBig.scale());
        assertEquals(1.5f, ShopViewport.fit(1920, 1080, 640, 392, 999).scale());
    }

    @Test void smallerPanelsStayCentered() {
        var viewport = ShopViewport.fit(640, 360, 640, 392, 60);
        assertEquals(640 - viewport.screenX(640), viewport.screenX(0), 1);
        assertEquals(360 - viewport.screenY(392), viewport.screenY(0), 1);
    }

    @Test void allSupportedGuiScalesKeepPanelsAndClickableCornersOnScreen() {
        for (int gui = 1; gui <= 8; gui++) {
            int width = (1920 + gui - 1) / gui, height = (1080 + gui - 1) / gui;
            for (int[] panel : new int[][] {{640, 392}, {760, 464}}) {
                var viewport = ShopViewport.fit(width, height, panel[0], panel[1]);
                assertTrue(viewport.left() >= 5.9f);
                assertTrue(viewport.top() >= 5.9f);
                assertTrue(viewport.screenX(panel[0]) <= width - 5);
                assertTrue(viewport.screenY(panel[1]) <= height - 5);
                // The close button and farthest grid cell must use the same inverse as the renderer.
                for (int[] point : new int[][] {{24, 25}, {panel[0] - 35, panel[1] - 35}}) {
                    assertEquals(point[0], viewport.localX(point[0] * viewport.scale() + viewport.left()), 0.001);
                    assertEquals(point[1], viewport.localY(point[1] * viewport.scale() + viewport.top()), 0.001);
                }
            }
        }
    }
}
