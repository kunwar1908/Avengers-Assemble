package com.avengers3d;

import com.avengers3d.physics.AABB;
import com.avengers3d.physics.BoundingSphere;
import com.avengers3d.physics.Physics3D;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure-math tests: no OpenGL needed. */
class Physics3DTest {
    private static AABB box(float cx, float cy, float cz, float h) {
        return new AABB().setCenterHalfExtents(cx, cy, cz, h, h, h);
    }

    @Test void overlappingBoxesIntersect() { assertTrue(box(0, 0, 0, 1).intersects(box(1.5f, 0, 0, 1))); }
    @Test void separatedBoxesDoNot() { assertFalse(box(0, 0, 0, 1).intersects(box(2.5f, 0, 0, 1))); }
    @Test void separationOnOneAxisIsEnough() { assertFalse(box(0, 0, 0, 1).intersects(box(0.5f, 3f, 0.5f, 1))); }

    @Test void sphereVsSphere() {
        BoundingSphere a = new BoundingSphere().set(0, 0, 0, 1), b = new BoundingSphere().set(1.9f, 0, 0, 1), c = new BoundingSphere().set(2.1f, 0, 0, 1);
        assertTrue(Physics3D.sphereVsSphere(a, b));
        assertFalse(Physics3D.sphereVsSphere(a, c));
    }

    @Test void sphereVsAabbUsesClosestPoint() {
        AABB b = box(0, 0, 0, 1);
        assertTrue(Physics3D.sphereVsAabb(new BoundingSphere().set(2.4f, 0, 0, 1.5f), b));
        assertFalse(Physics3D.sphereVsAabb(new BoundingSphere().set(3f, 3f, 0, 1.5f), b));
    }

    @Test void penetrationPicksTheSmallestAxis() {
        Vector3f mtv = new Vector3f();
        // A overlaps B by 0.2 on X (smallest) and by 1.5+ on Y/Z
        assertTrue(Physics3D.aabbPenetration(box(1.8f, 0, 0, 1), box(0, 0, 0, 1), mtv));
        assertEquals(0.2f, mtv.x, 1e-4f);
        assertEquals(0f, mtv.y, 1e-6f);
        assertFalse(Physics3D.aabbPenetration(box(5, 0, 0, 1), box(0, 0, 0, 1), mtv));
    }

    @Test void rayHitsBoxAtExpectedDistance() {
        float t = Physics3D.raycastAabb(new Vector3f(-5, 0, 0), new Vector3f(1, 0, 0), box(0, 0, 0, 1), 100f);
        assertEquals(4f, t, 1e-4f);
    }

    @Test void rayMissesAndRespectsMaxDistance() {
        assertEquals(-1f, Physics3D.raycastAabb(new Vector3f(-5, 5, 0), new Vector3f(1, 0, 0), box(0, 0, 0, 1), 100f));
        assertEquals(-1f, Physics3D.raycastAabb(new Vector3f(-5, 0, 0), new Vector3f(1, 0, 0), box(0, 0, 0, 1), 3f));
    }
}
