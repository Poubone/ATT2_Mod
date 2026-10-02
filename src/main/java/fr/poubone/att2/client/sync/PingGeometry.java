package fr.poubone.att2.client.sync;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Projection and sizing adapted from Ping Wheel (MIT, see LICENSE_pingwheel). */
public final class PingGeometry {
    private PingGeometry() {}

    public record ScreenPos(float x, float y, boolean behind) {
        public boolean onScreen(int width, int height) {
            return !behind && x >= 0 && x <= width && y >= 0 && y <= height;
        }
    }

    public static ScreenPos project(float x, float y, float z, Matrix4f view, Matrix4f projection,
                                    int width, int height) {
        Vector4f clip = new Vector4f(x, y, z, 1).mul(view).mul(projection);
        float depth = clip.w;
        float divisor = Math.abs(depth) < 0.0001f ? (depth < 0 ? -0.0001f : 0.0001f) : depth;
        return new ScreenPos(width * (0.5f + clip.x / divisor * 0.5f),
                height * (0.5f - clip.y / divisor * 0.5f), depth <= 0);
    }

    public static float scale(double distance) {
        return (float) Math.max(1.0, 2.0 / Math.pow(Math.max(0.5, distance), 0.3)) * 0.5f;
    }

    public record Edge(float x, float y, float angle) {}

    public static Edge edge(ScreenPos pos, int width, int height) {
        float left = 5, right = Math.max(6, width - 5);
        float top = 5, bottom = Math.max(6, height - 60);
        float cx = (left + right) / 2, cy = (top + bottom) / 2;
        float dx = pos.x - cx, dy = pos.y - cy;
        if (pos.behind) { dx = -dx; dy = -dy; }
        if (Math.abs(dx) + Math.abs(dy) < 0.001f) dy = 1;
        float t = Math.min((right - left) / 2 / Math.max(0.0001f, Math.abs(dx)),
                (bottom - top) / 2 / Math.max(0.0001f, Math.abs(dy)));
        return new Edge(cx + dx * t, cy + dy * t, (float) Math.atan2(dy, dx));
    }
}
