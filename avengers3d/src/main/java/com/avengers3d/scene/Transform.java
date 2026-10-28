package com.avengers3d.scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Position / rotation / scale with a cached model matrix.
 *
 * Model = T * R * S (applied right-to-left to a column vector): scale first, then rotate
 * about the local origin, then translate into the world. Rotation is a quaternion, which
 * avoids gimbal lock and interpolates cleanly (slerp). Euler angles are accepted only as a
 * convenience for authoring.
 */
public final class Transform {
    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1f);
    private final Matrix4f model = new Matrix4f();
    private boolean dirty = true;

    public Vector3f position() { return position; }   // read-only by convention: mutate via setters
    public Vector3f scale() { return scale; }
    public Quaternionf rotation() { return rotation; }

    public void setPosition(float x, float y, float z) { position.set(x, y, z); dirty = true; }
    public void translate(float dx, float dy, float dz) { position.add(dx, dy, dz); dirty = true; }
    public void setScale(float x, float y, float z) { scale.set(x, y, z); dirty = true; }
    public void setEuler(float pitch, float yaw, float roll) { rotation.rotationYXZ(yaw, pitch, roll); dirty = true; }
    public void rotateY(float radians) { rotation.rotateY(radians); dirty = true; }
    public void rotateLocalX(float radians) { rotation.rotateX(radians); dirty = true; }

    /** Heading about +Y (model faces +Z at yaw 0). */
    public void setYaw(float yaw) { rotation.rotationY(yaw); dirty = true; }

    /** Rebuilt only when something changed. */
    public Matrix4f modelMatrix() {
        if (dirty) {
            model.translationRotateScale(position, rotation, scale);
            dirty = false;
        }
        return model;
    }
}
