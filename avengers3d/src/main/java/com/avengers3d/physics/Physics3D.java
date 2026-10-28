package com.avengers3d.physics;

import com.avengers3d.scene.GameObject3D;
import org.joml.Vector3f;

/**
 * Manual 3D collision + vertical physics. Static, allocation-free, no external engine.
 * World convention: +Y is up, units are metres, right-handed coordinates.
 */
public final class Physics3D {
    public static final float GRAVITY = -25f;   // arcade-feel gravity (m/s^2); real Earth = -9.81
    private Physics3D() { }

    // ---- integration ------------------------------------------------------
    /**
     * Semi-implicit (symplectic) Euler: update velocity first, then position with the NEW velocity.
     *     v' = v + a*dt        p' = p + v'*dt
     * More stable than explicit Euler for gravity/spring-like forces, and just as cheap.
     */
    public static void integrate(GameObject3D o, float dt, float groundY) {
        if (o.useGravity) o.velocity.y += GRAVITY * dt;
        o.transform().translate(o.velocity.x * dt, o.velocity.y * dt, o.velocity.z * dt);

        Vector3f p = o.transform().position();
        if (p.y <= groundY) {                                  // floor constraint
            o.transform().setPosition(p.x, groundY, p.z);
            if (o.velocity.y < 0) o.velocity.y = 0;            // kill downward velocity on landing
            o.grounded = true;
        } else {
            o.grounded = false;
        }
    }

    /** Clamp an object inside an arena box so characters can't leave the level. */
    public static void clampToBounds(GameObject3D o, AABB arena) {
        Vector3f p = o.transform().position();
        o.transform().setPosition(
            Math.max(arena.min.x, Math.min(arena.max.x, p.x)),
            Math.max(arena.min.y, Math.min(arena.max.y, p.y)),
            Math.max(arena.min.z, Math.min(arena.max.z, p.z)));
    }

    // ---- overlap tests ----------------------------------------------------
    /** |c1 - c2|^2 <= (r1 + r2)^2 : compare squared distances to avoid a sqrt. */
    public static boolean sphereVsSphere(BoundingSphere a, BoundingSphere b) {
        float r = a.radius + b.radius;
        return a.center.distanceSquared(b.center) <= r * r;
    }

    /**
     * Sphere vs AABB: find the point on the box closest to the sphere centre by clamping the
     * centre to [min, max] per axis, then test that distance against the radius.
     */
    public static boolean sphereVsAabb(BoundingSphere s, AABB b) {
        float cx = Math.max(b.min.x, Math.min(s.center.x, b.max.x));
        float cy = Math.max(b.min.y, Math.min(s.center.y, b.max.y));
        float cz = Math.max(b.min.z, Math.min(s.center.z, b.max.z));
        return s.center.distanceSquared(cx, cy, cz) <= s.radius * s.radius;
    }

    /**
     * AABB vs AABB with a minimum translation vector (MTV).
     * Per axis, overlap = min(maxA, maxB) - max(minA, minB). The axis with the SMALLEST overlap is
     * the cheapest way to separate the boxes. Writes the push-out vector for A into {@code mtv}.
     * @return true if the boxes overlap
     */
    public static boolean aabbPenetration(AABB a, AABB b, Vector3f mtv) {
        float ox = Math.min(a.max.x, b.max.x) - Math.max(a.min.x, b.min.x);
        float oy = Math.min(a.max.y, b.max.y) - Math.max(a.min.y, b.min.y);
        float oz = Math.min(a.max.z, b.max.z) - Math.max(a.min.z, b.min.z);
        if (ox <= 0 || oy <= 0 || oz <= 0) return false;       // separating axis found

        float acx = a.min.x + a.max.x, bcx = b.min.x + b.max.x; // 2x centres, avoids dividing by 2
        float acy = a.min.y + a.max.y, bcy = b.min.y + b.max.y;
        float acz = a.min.z + a.max.z, bcz = b.min.z + b.max.z;

        if (ox <= oy && ox <= oz)      mtv.set(acx < bcx ? -ox : ox, 0, 0);
        else if (oy <= oz)             mtv.set(0, acy < bcy ? -oy : oy, 0);
        else                           mtv.set(0, 0, acz < bcz ? -oz : oz);
        return true;
    }

    /**
     * Resolve a dynamic object against a static box (platform, building, wall).
     * Landing on top is detected when the MTV points +Y and the object is falling.
     */
    public static boolean resolveAgainstStatic(GameObject3D o, AABB solid, Vector3f scratchMtv) {
        AABB box = o.worldBounds();
        if (!aabbPenetration(box, solid, scratchMtv)) return false;

        o.transform().translate(scratchMtv.x, scratchMtv.y, scratchMtv.z);
        if (scratchMtv.y > 0 && o.velocity.y <= 0) { o.velocity.y = 0; o.grounded = true; }   // landed on top
        else if (scratchMtv.y < 0 && o.velocity.y > 0) { o.velocity.y = 0; }                  // bumped head
        if (scratchMtv.x != 0) o.velocity.x = 0;
        if (scratchMtv.z != 0) o.velocity.z = 0;
        return true;
    }

    // ---- raycasting -------------------------------------------------------
    /**
     * Ray vs AABB using the slab method. For each axis the ray enters and leaves the slab
     * between planes min and max at t = (plane - origin) / dir. The ray hits the box iff the
     * LATEST entry time is before the EARLIEST exit time.
     * @return distance t along the ray (dir should be normalized), or -1 if no hit within maxDist
     */
    public static float raycastAabb(Vector3f origin, Vector3f dir, AABB box, float maxDist) {
        float tEnter = 0f, tExit = maxDist;
        for (int axis = 0; axis < 3; axis++) {
            float o = origin.get(axis), d = dir.get(axis);
            float lo = box.min.get(axis), hi = box.max.get(axis);
            if (Math.abs(d) < 1e-8f) {                       // ray parallel to this slab
                if (o < lo || o > hi) return -1f;            // and outside it: can never hit
            } else {
                float t1 = (lo - o) / d, t2 = (hi - o) / d;
                if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
                tEnter = Math.max(tEnter, t1);
                tExit = Math.min(tExit, t2);
                if (tEnter > tExit) return -1f;
            }
        }
        return tEnter;
    }
}
