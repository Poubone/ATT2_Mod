package fr.poubone.att2.client.sync;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PingGeometryTest {
    private final Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70), 16f / 9, 0.05f, 2048);

    @Test void pointAheadIsAtCrosshairAndPointBehindIsFlagged() {
        var front = PingGeometry.project(0, 0, -10, new Matrix4f(), projection, 320, 180);
        assertEquals(160, front.x(), 0.001);
        assertEquals(90, front.y(), 0.001);
        assertTrue(front.onScreen(320, 180));
        assertTrue(PingGeometry.project(0, 0, 10, new Matrix4f(), projection, 320, 180).behind());
    }

    @Test void projectionUsesActualFovAndCameraRotation() {
        var normal = PingGeometry.project(5, 0, -10, new Matrix4f(), projection, 320, 180);
        var wide = PingGeometry.project(5, 0, -10, new Matrix4f(),
                new Matrix4f().perspective((float) Math.toRadians(100), 16f / 9, 0.05f, 2048), 320, 180);
        assertTrue(wide.x() < normal.x());
        var turned = PingGeometry.project(10, 0, 0, new Matrix4f().rotateY((float) Math.PI / 2), projection, 320, 180);
        assertEquals(160, turned.x(), 0.001);
        assertFalse(turned.behind());
    }

    @Test void offscreenArrowsStayInsideSafeZoneAndPreserveDirection() {
        var right = PingGeometry.edge(new PingGeometry.ScreenPos(900, 90, false), 320, 180);
        assertEquals(315, right.x(), 0.001);
        assertTrue(right.y() >= 5 && right.y() <= 120);
        var behind = PingGeometry.edge(new PingGeometry.ScreenPos(900, 90, true), 320, 180);
        assertEquals(5, behind.x(), 0.001);
        var vertical = PingGeometry.edge(new PingGeometry.ScreenPos(160, -1000, false), 320, 180);
        assertEquals(5, vertical.y(), 0.001);
    }

    @Test void nearbyPingsAreLargerButDistantPingsRemainReadable() {
        assertTrue(PingGeometry.scale(1) > PingGeometry.scale(10));
        assertEquals(0.5f, PingGeometry.scale(1000));
        assertTrue(Float.isFinite(PingGeometry.scale(0)));
    }
}
