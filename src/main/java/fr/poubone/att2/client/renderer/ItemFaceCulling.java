package fr.poubone.att2.client.renderer;

import com.lootbeams.compat.iris.IrisCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD config {@code cullHiddenItemFaces}: the quads of a dropped item that face away from the camera are left out
 * before they are written. Item pipelines cull back faces on the GPU, so the picture is the same; those vertices are
 * just never written, sorted, uploaded and discarded. A glowing item with glint saves it three times, as its glint
 * and outline passes draw the same list.
 *
 * <p>The test runs in the item's model space against the camera, which sits at the origin of the pose's space. A face
 * is only left out when it is turned away from every point within {@link #EYE_SLACK} of the camera, so view bobbing
 * and float rounding can keep a face the GPU would discard, never drop one it would draw.
 */
public final class ItemFaceCulling {
    /** Below this many quads the test saves less than it costs. */
    private static final int MIN_QUADS = 8;
    /** Blocks around the camera the eye may really be at (view bobbing moves it a little). */
    private static final float EYE_SLACK = 0.25f;
    private static final boolean IRIS = IrisCompat.isIrisLoaded();

    private static final List<BakedQuad> visible = new ArrayList<>();
    private static final Matrix4f inverse = new Matrix4f();
    private static final Matrix4f lastPose = new Matrix4f();
    private static List<BakedQuad> lastQuads;

    private ItemFaceCulling() {
    }

    /** The quads of one item submit that can be seen; the outline pass right after reuses the same answer. */
    public static List<BakedQuad> visible(ItemDisplayContext context, PoseStack.Pose pose, List<BakedQuad> quads, RenderType renderType) {
        if (context != ItemDisplayContext.GROUND || quads.size() < MIN_QUADS || !HUDConfig.get().cullHiddenItemFaces
                || !renderType.pipeline().isCull()
                // A shader pack's shadow pass draws the same items as seen from the sun.
                || IRIS && IrisCompat.isShaderPackInUse()) {
            return quads;
        }
        Matrix4f matrix = pose.pose();
        if (quads == lastQuads && matrix.equals(lastPose)) {
            return visible;
        }
        float det = matrix.determinant3x3();
        if (!(Math.abs(det) > 1.0e-12f)) {
            return quads;
        }
        inverse.set(matrix).invertAffine();
        // The camera in model space, and the eye slack in model units.
        float cx = inverse.m30(), cy = inverse.m31(), cz = inverse.m32();
        float slack = EYE_SLACK / smallestScale(matrix, det);
        float slackSq = slack * slack;
        float facing = det > 0 ? 1.0f : -1.0f; // a mirroring transform flips which side is the front

        visible.clear();
        for (int i = 0, n = quads.size(); i < n; i++) {
            BakedQuad quad = quads.get(i);
            if (!turnedAway(quad.position0(), quad.position1(), quad.position2(), quad.position3(), cx, cy, cz, facing, slackSq)) {
                visible.add(quad);
            }
        }
        lastQuads = quads;
        lastPose.set(matrix);
        return visible;
    }

    /**
     * Whether a quad faces away from the eye at (cx, cy, cz) by more than the slack (squared, in the same units). The
     * front is the side its corners wind counter-clockwise on; {@code facing} is -1 to swap sides.
     */
    static boolean turnedAway(Vector3fc p0, Vector3fc p1, Vector3fc p2, Vector3fc p3, float cx, float cy, float cz, float facing,
                              float slackSq) {
        // The face's normal from its winding: the cross product of the diagonals.
        float ax = p2.x() - p0.x(), ay = p2.y() - p0.y(), az = p2.z() - p0.z();
        float bx = p3.x() - p1.x(), by = p3.y() - p1.y(), bz = p3.z() - p1.z();
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float toward = facing * (nx * (cx - p0.x()) + ny * (cy - p0.y()) + nz * (cz - p0.z()));
        return toward < 0 && toward * toward > slackSq * (nx * nx + ny * ny + nz * nz);
    }

    /**
     * The least a model unit is stretched to by the transform's 3 x 3 part: the shortest column when the columns are
     * square to each other (rotations and axis scales, which is what items use), else a lower bound for any matrix.
     */
    static float smallestScale(Matrix4f m, float det) {
        float c0 = m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02();
        float c1 = m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12();
        float c2 = m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22();
        float d01 = m.m00() * m.m10() + m.m01() * m.m11() + m.m02() * m.m12();
        float d02 = m.m00() * m.m20() + m.m01() * m.m21() + m.m02() * m.m22();
        float d12 = m.m10() * m.m20() + m.m11() * m.m21() + m.m12() * m.m22();
        float tolerance = 1.0e-6f;
        if (d01 * d01 <= tolerance * c0 * c1 && d02 * d02 <= tolerance * c0 * c2 && d12 * d12 <= tolerance * c1 * c2) {
            return (float) Math.sqrt(Math.min(c0, Math.min(c1, c2)));
        }
        // The smallest singular value is at least |det| divided by the square of the largest, itself below the norm.
        return Math.abs(det) / (c0 + c1 + c2);
    }
}
