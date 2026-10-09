package fr.poubone.att2.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFaceCullingTest {
    /** A unit quad in the z = 0 plane, wound counter-clockwise seen from +z: its front faces +z. */
    private static final Vector3f P0 = new Vector3f(0, 0, 0), P1 = new Vector3f(1, 0, 0), P2 = new Vector3f(1, 1, 0),
            P3 = new Vector3f(0, 1, 0);

    private static boolean away(float x, float y, float z, float slack) {
        return ItemFaceCulling.turnedAway(P0, P1, P2, P3, x, y, z, 1, slack * slack);
    }

    @Test void keepsAFaceSeenFromItsFront() {
        assertFalse(away(0.5f, 0.5f, 3, 0.1f));
    }

    @Test void dropsAFaceSeenFromBehind() {
        assertTrue(away(0.5f, 0.5f, -3, 0.1f));
    }

    @Test void keepsAFaceTheEyeCouldStillSeeWithinTheSlack() {
        // 0.05 behind the face's plane: an eye moved by the slack could be in front of it.
        assertFalse(away(0.5f, 0.5f, -0.05f, 0.1f));
        assertTrue(away(0.5f, 0.5f, -0.15f, 0.1f));
    }

    @Test void keepsAFaceSeenEdgeOn() {
        assertFalse(away(5, 0.5f, 0, 0.1f));
    }

    @Test void aMirroringTransformSwapsTheFront() {
        assertTrue(ItemFaceCulling.turnedAway(P0, P1, P2, P3, 0.5f, 0.5f, 3, -1, 0.01f));
        assertFalse(ItemFaceCulling.turnedAway(P0, P1, P2, P3, 0.5f, 0.5f, -3, -1, 0.01f));
    }

    @Test void aDegenerateQuadIsKept() {
        Vector3f p = new Vector3f(0.5f, 0.5f, 0);
        assertFalse(ItemFaceCulling.turnedAway(p, p, p, p, 0.5f, 0.5f, -3, 1, 0));
    }

    @Test void smallestScaleOfARotatedUniformScaleIsTheScale() {
        Matrix4f m = new Matrix4f().translate(3, -2, 7).rotateY(1.1f).rotateX(0.3f).scale(0.5f);
        assertEquals(0.5f, ItemFaceCulling.smallestScale(m, m.determinant3x3()), 1e-5f);
    }

    @Test void smallestScaleOfAnAxisScaleIsItsShortestAxis() {
        Matrix4f m = new Matrix4f().rotateZ(0.7f).scale(2, 0.25f, 1);
        assertEquals(0.25f, ItemFaceCulling.smallestScale(m, m.determinant3x3()), 1e-5f);
    }

    @Test void smallestScaleOfASkewStaysBelowTheTrueValue() {
        Matrix4f m = new Matrix4f(1, 0, 0, 0, 0.9f, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1); // shear
        // The skew's smallest singular value is about 0.6466; the bound must not exceed it.
        float bound = ItemFaceCulling.smallestScale(m, m.determinant3x3());
        assertTrue(bound > 0 && bound <= 0.6466f, "bound " + bound);
    }
}
