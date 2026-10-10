package fr.poubone.att2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.poubone.att2.mixin.LayerRenderStateAccessor;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * HUD config {@code cacheItemBounds}: an item's model bounds, which vanilla works out every frame for every dropped
 * item by moving each of the model's points through its display transform. A layer's bounds only depend on its point
 * set and transform, so they are kept per pair; the union over layers is the same box vanilla builds point by point.
 */
public final class ItemBounds {
    /**
     * Point set -> transform -> {right hand, left hand} bounds. Point sets are the models' memoized arrays, so the weak
     * identity keys go with their model on a resource reload.
     */
    private static final Map<Vector3fc[], Map<ItemTransform, AABB[]>> CACHE = new WeakHashMap<>();

    private ItemBounds() {
    }

    /** The bounds of the first {@code count} layers, or null when none has points (vanilla handles that case). */
    public static AABB of(ItemStackRenderState.LayerRenderState[] layers, int count, boolean leftHand) {
        AABB union = null;
        for (int i = 0; i < count; i++) {
            LayerRenderStateAccessor layer = (LayerRenderStateAccessor) layers[i];
            Vector3fc[] points = layer.att2$extents().get();
            if (points.length == 0) continue;
            AABB box = layerBounds(points, layer.att2$transform(), leftHand);
            union = union == null ? box : union.minmax(box);
        }
        return union;
    }

    static AABB layerBounds(Vector3fc[] points, ItemTransform transform, boolean leftHand) {
        AABB[] boxes = CACHE.computeIfAbsent(points, k -> new IdentityHashMap<>()).computeIfAbsent(transform, k -> new AABB[2]);
        int side = leftHand ? 1 : 0;
        AABB box = boxes[side];
        if (box == null) {
            PoseStack.Pose pose = new PoseStack.Pose();
            transform.apply(leftHand, pose);
            Matrix4f matrix = pose.pose();
            Vector3f point = new Vector3f();
            AABB.Builder builder = new AABB.Builder();
            for (Vector3fc p : points) {
                builder.include(point.set(p).mulPosition(matrix));
            }
            box = builder.build();
            boxes[side] = box;
        }
        return box;
    }
}
