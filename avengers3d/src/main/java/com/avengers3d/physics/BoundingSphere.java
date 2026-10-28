package com.avengers3d.physics;

import org.joml.Vector3f;

public final class BoundingSphere {
    public final Vector3f center = new Vector3f();
    public float radius;

    public BoundingSphere set(float x, float y, float z, float r) { center.set(x, y, z); radius = r; return this; }
}
