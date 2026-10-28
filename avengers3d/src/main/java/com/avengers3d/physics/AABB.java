package com.avengers3d.physics;

import org.joml.Vector3f;

/** Axis-aligned bounding box stored as min/max corners. Mutable and reusable. */
public final class AABB {
    public final Vector3f min = new Vector3f();
    public final Vector3f max = new Vector3f();

    public AABB setCenterHalfExtents(float cx, float cy, float cz, float hx, float hy, float hz) {
        min.set(cx - hx, cy - hy, cz - hz);
        max.set(cx + hx, cy + hy, cz + hz);
        return this;
    }

    /**
     * Separating Axis Theorem for boxes: two AABBs are disjoint iff their intervals are
     * disjoint on at least one of the x, y, z axes. They intersect only if they overlap on ALL three.
     */
    public boolean intersects(AABB o) {
        return min.x <= o.max.x && max.x >= o.min.x
            && min.y <= o.max.y && max.y >= o.min.y
            && min.z <= o.max.z && max.z >= o.min.z;
    }

    public boolean contains(float x, float y, float z) {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z;
    }
}
