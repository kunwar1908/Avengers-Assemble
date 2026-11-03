package com.avengers3d.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera with first/third-person modes, smooth follow, screen shake and FOV kick.
 *
 * Orientation is yaw (about +Y) and pitch. Forward from spherical coordinates (yaw 0 looks down -Z):
 *     f = ( sin(yaw)cos(pitch),  sin(pitch), -cos(yaw)cos(pitch) )
 * Basis by cross products: right = normalize(f x worldUp), up = right x f.
 * View = inverse of the camera's world transform (built by lookAt); Projection maps the view frustum to clip space.
 * The inverse view-projection is kept too: the sky shader uses it to turn a pixel back into a world-space ray.
 */
public final class Camera3D {
    public enum Mode { FIRST_PERSON, THIRD_PERSON }

    private static final Vector3f WORLD_UP = new Vector3f(0, 1, 0);
    private static final float MAX_PITCH = (float) Math.toRadians(85);

    private final Vector3f anchor = new Vector3f(), target = new Vector3f();
    private float yaw, pitch;
    private float fov = (float) Math.toRadians(70), fovBoost, fovBoostTarget, aspect = 16f / 9f, near = 0.1f, far = 500f;
    private float eyeHeight = 1.7f, shoulderHeight = 1.8f, orbitDistance = 8f, shoulderOffset = 0.8f;
    private float shake, time;
    private Mode mode = Mode.THIRD_PERSON;

    private final Vector3f forward = new Vector3f(0, 0, -1), right = new Vector3f(1, 0, 0), up = new Vector3f(0, 1, 0);
    private final Vector3f eye = new Vector3f(), focus = new Vector3f();
    private final Matrix4f projection = new Matrix4f(), view = new Matrix4f(),
                           viewProjection = new Matrix4f(), invViewProjection = new Matrix4f();

    // ---- configuration ----------------------------------------------------
    public void setLens(float fovRadians, float aspect, float near, float far) {
        this.fov = fovRadians; this.aspect = aspect; this.near = near; this.far = far;
    }
    public void setAspect(float width, float height) { aspect = width / height; }
    public void configure(float eyeHeight, float shoulderHeight, float orbitDistance) {
        this.eyeHeight = eyeHeight; this.shoulderHeight = shoulderHeight; this.orbitDistance = orbitDistance;
    }
    public void setShoulderOffset(float v) { shoulderOffset = v; }
    public void setAngles(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }
    public void toggleMode() { mode = (mode == Mode.FIRST_PERSON) ? Mode.THIRD_PERSON : Mode.FIRST_PERSON; }
    public Mode mode() { return mode; }
    public boolean firstPerson() { return mode == Mode.FIRST_PERSON; }

    // ---- input ------------------------------------------------------------
    public void rotate(float dxPixels, float dyPixels, float sensitivity) {
        yaw += dxPixels * sensitivity;
        pitch -= dyPixels * sensitivity;
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
    }
    public void zoom(float scroll) { orbitDistance = Math.max(3f, Math.min(22f, orbitDistance - scroll)); }

    /** Where the camera wants to look (the avatar's feet). The camera glides there. */
    public void follow(Vector3f p) { target.set(p); }
    public void snap() { anchor.set(target); }
    public void addShake(float amount) { shake = Math.min(1.5f, shake + amount); }
    public void setFovBoost(float radians) { fovBoostTarget = radians; }

    public Vector3f flatForward(Vector3f dest) { return dest.set((float) Math.sin(yaw), 0, (float) -Math.cos(yaw)); }
    public Vector3f flatRight(Vector3f dest) { return dest.set((float) Math.cos(yaw), 0, (float) Math.sin(yaw)); }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    // ---- matrices ---------------------------------------------------------
    public void update(float dt) {
        time += dt;
        anchor.lerp(target, 1f - (float) Math.exp(-22f * dt));            // frame-rate independent smoothing
        fovBoost += (fovBoostTarget - fovBoost) * (1f - (float) Math.exp(-6f * dt));
        shake *= (float) Math.exp(-7f * dt);

        float cp = (float) Math.cos(pitch);
        forward.set((float) Math.sin(yaw) * cp, (float) Math.sin(pitch), (float) -Math.cos(yaw) * cp).normalize();
        forward.cross(WORLD_UP, right).normalize();
        right.cross(forward, up);

        if (mode == Mode.FIRST_PERSON) {
            eye.set(anchor.x, anchor.y + eyeHeight, anchor.z);
            focus.set(eye).add(forward);
        } else {
            // Over-the-shoulder orbit: look at a point beside the avatar's shoulder, sit behind along -forward.
            focus.set(anchor.x, anchor.y + shoulderHeight, anchor.z).fma(shoulderOffset, right);
            eye.set(focus).fma(-orbitDistance, forward);
        }
        if (shake > 0.002f) {
            float sx = (float) Math.sin(time * 63f) * shake * 0.22f, sy = (float) Math.sin(time * 71f + 1.3f) * shake * 0.22f;
            eye.fma(sx, right).fma(sy, up);
            focus.fma(sx, right).fma(sy, up);
        }
        view.setLookAt(eye, focus, WORLD_UP);
        projection.setPerspective(fov + fovBoost, aspect, near, far);
        projection.mul(view, viewProjection);                              // VP = P * V
        viewProjection.invert(invViewProjection);
    }

    public Matrix4f view() { return view; }
    public Matrix4f projection() { return projection; }
    public Matrix4f viewProjection() { return viewProjection; }
    public Matrix4f invViewProjection() { return invViewProjection; }
    public Vector3f eye() { return eye; }
    public Vector3f forward() { return forward; }
    public Vector3f right() { return right; }
    public Vector3f up() { return up; }
}
