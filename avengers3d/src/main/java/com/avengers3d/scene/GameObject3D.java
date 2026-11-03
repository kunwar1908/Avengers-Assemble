package com.avengers3d.scene;

import com.avengers3d.physics.AABB;
import com.avengers3d.render.Mesh;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for everything in the world. Origin = FEET (bottom centre), so ground contact is y == groundY.
 *
 * Visuals are a hierarchy of Parts (a skeleton): each Part has a joint (pivot) positioned relative to its
 * parent joint, an animated rotation, and optional geometry hanging off the joint:
 *     jointWorld = parent.jointWorld * T(joint + offset) * R(pose)
 *     meshWorld  = jointWorld * T(geometry centre) * S(size)
 * Rotating a shoulder therefore swings the whole arm, the hand and any prop held in it.
 */
public abstract class GameObject3D {
    public static final class Part {
        public final Mesh mesh;                 // null = joint only (no geometry)
        public final Part parent;
        public final float jx, jy, jz;          // joint position in the parent's joint space
        public final float ox, oy, oz;          // geometry centre relative to the joint
        public final float sx, sy, sz;          // geometry size
        public float offX, offY, offZ;          // animated joint translation
        public float rotX, rotY, rotZ;          // animated joint rotation in radians (applied as Y * X * Z)
        public float vsx = 1f, vsy = 1f, vsz = 1f;   // visual scale multipliers (squash and stretch)
        public float r, g, b;                   // tint
        public float emissive, spec = 0.25f, texScale;
        public int textureId = -1;              // -1 = use the object's texture
        public boolean visible = true, castShadow = true;
        public final Matrix4f jointWorld = new Matrix4f(), meshWorld = new Matrix4f();
        public final Matrix3f normalMat = new Matrix3f();

        private Part(Mesh mesh, Part parent, float jx, float jy, float jz, float ox, float oy, float oz,
                     float sx, float sy, float sz, float r, float g, float b) {
            this.mesh = mesh; this.parent = parent;
            this.jx = jx; this.jy = jy; this.jz = jz; this.ox = ox; this.oy = oy; this.oz = oz;
            this.sx = sx; this.sy = sy; this.sz = sz; this.r = r; this.g = g; this.b = b;
        }
    }

    private static int nextId = 0;

    public final int id = nextId++;
    public final Vector3f velocity = new Vector3f();
    public boolean useGravity = true, grounded, castShadow = true;
    public int textureId;
    public float flash;                         // 0..1 white flash (hit feedback)
    public float rim;                           // rim-light strength (heroes get a heroic silhouette)
    public final Vector3f visualScale = new Vector3f(1f, 1f, 1f);   // renderer-only scale: does NOT change the collider
    public final List<Part> parts = new ArrayList<>();

    protected final Transform transform = new Transform();
    protected final Vector3f halfExtents = new Vector3f(0.5f, 1f, 0.5f);
    private final AABB bounds = new AABB();
    private final Matrix4f base = new Matrix4f();

    public Transform transform() { return transform; }

    public Part addPart(Mesh mesh, Part parent, float jx, float jy, float jz, float ox, float oy, float oz,
                        float sx, float sy, float sz, float r, float g, float b) {
        Part p = new Part(mesh, parent, jx, jy, jz, ox, oy, oz, sx, sy, sz, r, g, b);
        parts.add(p);
        return p;
    }

    /** A rigid box with its pivot at its own centre (props, blocks, projectiles). */
    public Part addBox(Mesh mesh, float cx, float cy, float cz, float sx, float sy, float sz, float r, float g, float b) {
        return addPart(mesh, null, cx, cy, cz, 0, 0, 0, sx, sy, sz, r, g, b);
    }

    /** World-space AABB: local half-extents scaled by the transform, anchored at the feet. */
    public AABB worldBounds() {
        Vector3f p = transform.position(), s = transform.scale();
        float hx = halfExtents.x * s.x, hy = halfExtents.y * s.y, hz = halfExtents.z * s.z;
        return bounds.setCenterHalfExtents(p.x, p.y + hy, p.z, hx, hy, hz);
    }

    /** Rebuild every part's matrices (parents are always added before children, so one pass is enough). */
    public void updateParts() {
        base.set(transform.modelMatrix()).scale(visualScale.x, visualScale.y, visualScale.z);
        for (int i = 0; i < parts.size(); i++) {
            Part p = parts.get(i);
            p.jointWorld.set(p.parent == null ? base : p.parent.jointWorld)
                        .translate(p.jx + p.offX, p.jy + p.offY, p.jz + p.offZ)
                        .rotateYXZ(p.rotY, p.rotX, p.rotZ);
            p.meshWorld.set(p.jointWorld).translate(p.ox, p.oy, p.oz).scale(p.sx * p.vsx, p.sy * p.vsy, p.sz * p.vsz);
            p.meshWorld.normal(p.normalMat);
        }
    }

    public abstract void update(float dt);
}
