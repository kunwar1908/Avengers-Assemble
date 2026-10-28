package com.avengers3d.scene;

import com.avengers3d.physics.AABB;
import com.avengers3d.render.Mesh;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for everything placed in the 3D world.
 * Origin convention: the transform position is the object's FEET (bottom centre), so ground
 * contact is simply position.y == groundY and the collider extends upward from there.
 *
 * Visuals are a list of {@link Part}s (box/sphere meshes with a local offset, size and colour).
 * Each part's matrix is  Model(object) * T(offset) * S(size)  - a tiny scene graph, which is how
 * a hero is built from a handful of primitives until real models/animation are plugged in.
 */
public abstract class GameObject3D {
    /** One drawable piece of an object, expressed in the object's local space. */
    public static final class Part {
        public final Mesh mesh;
        public final float ox, oy, oz, sx, sy, sz, r, g, b;
        private Part(Mesh mesh, float ox, float oy, float oz, float sx, float sy, float sz, float r, float g, float b) {
            this.mesh = mesh; this.ox = ox; this.oy = oy; this.oz = oz;
            this.sx = sx; this.sy = sy; this.sz = sz; this.r = r; this.g = g; this.b = b;
        }
    }

    private static int nextId = 0;

    public final int id = nextId++;
    public final Vector3f velocity = new Vector3f();
    public boolean useGravity = true;
    public boolean grounded;
    public int textureId;
    public float flash;                         // 0..1 white blend (hit flash)
    public final List<Part> parts = new ArrayList<>();

    protected final Transform transform = new Transform();
    protected final Vector3f halfExtents = new Vector3f(0.5f, 1f, 0.5f);  // local collider size
    private final AABB bounds = new AABB();

    public Transform transform() { return transform; }

    public GameObject3D addPart(Mesh mesh, float ox, float oy, float oz,
                                float sx, float sy, float sz, float r, float g, float b) {
        parts.add(new Part(mesh, ox, oy, oz, sx, sy, sz, r, g, b));
        return this;
    }

    /** World-space AABB: local half-extents scaled by the transform, anchored at the feet. */
    public AABB worldBounds() {
        Vector3f p = transform.position(), s = transform.scale();
        float hx = halfExtents.x * s.x, hy = halfExtents.y * s.y, hz = halfExtents.z * s.z;
        return bounds.setCenterHalfExtents(p.x, p.y + hy, p.z, hx, hy, hz);
    }

    public abstract void update(float dt);
}
