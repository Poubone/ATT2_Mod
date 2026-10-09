package fr.poubone.att2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemBoundsTest {
    /** Vanilla's ItemStackRenderState.visitExtents for one layer, point by point. */
    private static AABB vanilla(Vector3fc[] points, ItemTransform transform, boolean leftHand) {
        PoseStack.Pose pose = new PoseStack.Pose();
        transform.apply(leftHand, pose);
        AABB.Builder builder = new AABB.Builder();
        Vector3f point = new Vector3f();
        for (Vector3fc p : points) builder.include(point.set(p).mulPosition(pose.pose()));
        return builder.build();
    }

    private static Vector3fc[] points(long seed) {
        Random random = new Random(seed);
        Vector3fc[] points = new Vector3fc[300];
        for (int i = 0; i < points.length; i++) points[i] = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
        return points;
    }

    @Test void matchesVanillaForBothHands() {
        Vector3fc[] points = points(1);
        ItemTransform transform = new ItemTransform(new Vector3f(30, 45, 10), new Vector3f(0.1f, 0.2f, -0.05f), new Vector3f(0.5f, 0.5f, 0.5f));
        for (boolean leftHand : new boolean[] {false, true}) {
            assertEquals(vanilla(points, transform, leftHand), ItemBounds.layerBounds(points, transform, leftHand));
        }
    }

    @Test void remembersPerPointSetAndTransform() {
        Vector3fc[] points = points(2);
        ItemTransform transform = new ItemTransform(new Vector3f(0, 90, 0), new Vector3f(), new Vector3f(1, 1, 1));
        AABB first = ItemBounds.layerBounds(points, transform, false);
        assertSame(first, ItemBounds.layerBounds(points, transform, false));
        assertEquals(vanilla(points, ItemTransform.NO_TRANSFORM, false), ItemBounds.layerBounds(points, ItemTransform.NO_TRANSFORM, false));
    }
}
