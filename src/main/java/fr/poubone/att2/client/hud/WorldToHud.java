package fr.poubone.att2.client.hud;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

/** Projects a world position onto GUI-scaled HUD coordinates. */
public final class WorldToHud {
    private WorldToHud() {
    }

    public static Optional<float[]> project(Vec3 world) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameRenderer == null || client.getWindow() == null) {
            return Optional.empty();
        }
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        Vector3f local = new Vector3f(
                (float) (world.x - cam.x),
                (float) (world.y - cam.y),
                (float) (world.z - cam.z)
        );
        Quaternionf inverse = camera.rotation().conjugate(new Quaternionf());
        inverse.transform(local);
        // Camera looks down -Z: points in front have a negative Z.
        if (local.z >= -0.05f) {
            return Optional.empty();
        }

        float fovDeg = client.options.fov().get().intValue();
        float tan = (float) Math.tan(Math.toRadians(fovDeg) * 0.5);
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        float aspect = (float) sw / (float) sh;
        float ndcX = local.x / (-local.z * tan * aspect);
        float ndcY = local.y / (-local.z * tan);
        if (Math.abs(ndcX) > 1.35f || Math.abs(ndcY) > 1.35f) {
            return Optional.empty();
        }
        float x = (ndcX * 0.5f + 0.5f) * sw;
        float y = (1.0f - (ndcY * 0.5f + 0.5f)) * sh;
        return Optional.of(new float[]{x, y});
    }

    public static boolean isInView(Vec3 world, double maxDistanceSq) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        if (client.player.distanceToSqr(world) > maxDistanceSq) return false;
        Vec3 eye = client.player.getEyePosition();
        Vec3 look = client.player.getViewVector(1.0F);
        Vec3 to = world.subtract(eye);
        double len = to.length();
        if (len < 1.0E-4) return true;
        return look.dot(to.normalize()) > 0.35;
    }
}
