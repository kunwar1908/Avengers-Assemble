package com.avengers3d.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera with first/third-person modes.
 *
 * Linear algebra recap
 * --------------------
 * Orientation is stored as yaw (about +Y) and pitch (about camera right).
 * Forward vector from spherical coordinates (yaw 0 looks down -Z, OpenGL convention):
 *     f = ( sin(yaw)cos(pitch),  sin(pitch), -cos(yaw)cos(pitch) )
 * Orthonormal basis via cross products:
 *     right = normalize(f x worldUp),   up = right x f
 * View matrix = inverse of the camera's world transform. lookAt builds it as:
 *     [ r.x  r.y  r.z  -r.e ]
 *     [ u.x  u.y  u.z  -u.e ]      e = eye position, columns of the rotation are the basis
 *     [-f.x -f.y -f.z   f.e ]      (rows here because the inverse of a rotation is its transpose)
 *     [  0    0    0     1  ]
 * Perspective matrix maps the frustum to clip space; w' = -z_eye so the GPU's divide-by-w
 * produces perspective foreshortening.
 */
public final class Camera3D {
    public enum Mode { FIRST_PERSON, THIRD_PERSON }

    private static final Vector3f WORLD_UP = new Vector3f(0, 1, 0);
    private static final float MAX_PITCH = (float) Math.toRadians(89);

    // Anchor = the avatar's feet position (or a free-fly position for debugging).
    private final Vector3f anchor = new Vector3f();
    private float yaw, pitch;

    private float fov = (float) Math.toRadians(70), aspect = 16f / 9f, near = 0.1f, far = 500f;
    private float eyeHeight = 1.7f, shoulderHeight = 1.8f, orbitDistance = 8f;
    private Mode mode = Mode.THIRD_PERSON;

    private final Vector3f forward = new Vector3f(0, 0, -1);
    private final Vector3f right = new Vector3f(1, 0, 0);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Vector3f eye = new Vector3f();
    private final Vector3f focus = new Vector3f();

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f viewProjection = new Matrix4f();
    private boolean projectionDirty = true;

    // ---- configuration ----------------------------------------------------
    public void setLens(float fovRadians, float aspect, float near, float far) {
        this.fov = fovRadians; this.aspect = aspect; this.near = near; this.far = far;
        projectionDirty = true;
    }
    public void setAspect(float width, float height) { aspect = width / height; projectionDirty = true; }
    public void toggleMode() { mode = (mode == Mode.FIRST_PERSON) ? Mode.THIRD_PERSON : Mode.FIRST_PERSON; }
    public Mode mode() { return mode; }

    // ---- input ------------------------------------------------------------
    /** Mouse delta in pixels. Pitch is clamped so forward never becomes parallel to worldUp (gimbal flip). */
    public void rotate(float dxPixels, float dyPixels, float sensitivityRadPerPixel) {
        yaw += dxPixels * sensitivityRadPerPixel;
        pitch -= dyPixels * sensitivityRadPerPixel;
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
    }

    public void zoom(float scrollDelta) {
        orbitDistance = Math.max(2f, Math.min(20f, orbitDistance - scrollDelta));
    }

    /** Free-fly movement (debug / Iron Man flight cam): axes in [-1,1]. */
    public void flyMove(float forwardAxis, float strafeAxis, float verticalAxis, float speed, float dt) {
        float s = speed * dt;
        anchor.fma(forwardAxis * s, forward).fma(strafeAxis * s, right).fma(verticalAxis * s, WORLD_UP);
    }

    /** Make the camera follow an avatar. Call once per update after physics. */
    public void follow(Vector3f avatarFeetPosition) { anchor.set(avatarFeetPosition); }

    /** Ground-plane directions for WASD (so looking up doesn't make Hulk walk into the sky). */
    public Vector3f flatForward(Vector3f dest) { return dest.set((float) Math.sin(yaw), 0, (float) -Math.cos(yaw)); }
    public Vector3f flatRight(Vector3f dest)   { return dest.set((float) Math.cos(yaw), 0, (float)  Math.sin(yaw)); }
    public float yaw() { return yaw; }

    // ---- setup helpers ----
    public void configure(float eyeHeight, float shoulderHeight, float orbitDistance) {
        this.eyeHeight = eyeHeight; this.shoulderHeight = shoulderHeight; this.orbitDistance = orbitDistance;
    }
    public void setAngles(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }
    public Vector3f forward() { return forward; }

    // ---- matrices ---------------------------------------------------------
    /** Recompute basis + matrices. Call once per frame; allocates nothing. */
    public void update() {
        float cp = (float) Math.cos(pitch);
        forward.set((float) Math.sin(yaw) * cp, (float) Math.sin(pitch), (float) -Math.cos(yaw) * cp).normalize();
        forward.cross(WORLD_UP, right).normalize();
        right.cross(forward, up);

        if (mode == Mode.FIRST_PERSON) {
            eye.set(anchor.x, anchor.y + eyeHeight, anchor.z);
            focus.set(eye).add(forward);
        } else {
            // Orbit camera: look at the shoulder point, sit orbitDistance *behind* the look direction.
            focus.set(anchor.x, anchor.y + shoulderHeight, anchor.z);
            eye.set(focus).fma(-orbitDistance, forward);
        }
        view.setLookAt(eye, focus, WORLD_UP);

        if (projectionDirty) { projection.setPerspective(fov, aspect, near, far); projectionDirty = false; }
        projection.mul(view, viewProjection);   // VP = P * V (column-vector convention)
    }

    public Matrix4f view() { return view; }
    public Matrix4f projection() { return projection; }
    public Matrix4f viewProjection() { return viewProjection; }
    public Vector3f eye() { return eye; }

    /**
     * Mouse picking / aiming ray in world space: unproject two points (near & far plane) with
     * inverse(P*V). Feed the result into Physics3D.raycastAabb().
     */
    public void screenRay(float mouseX, float mouseY, int width, int height, Vector3f origin, Vector3f dir) {
        // GLFW mouse Y grows downward, GL window Y grows upward.
        viewProjection.unprojectRay(mouseX, height - mouseY, new int[]{0, 0, width, height}, origin, dir);
        // NOTE: the int[] is tiny; cache a field if you call this every frame.
    }
}
