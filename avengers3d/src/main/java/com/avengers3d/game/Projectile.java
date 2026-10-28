package com.avengers3d.game;

import com.avengers3d.render.Mesh;
import com.avengers3d.scene.GameObject3D;
import org.joml.Vector3f;

/** Pooled repulsor bolt. Straight-line motion, no gravity. */
public final class Projectile extends GameObject3D {
    private static final float R = 0.25f;
    public boolean active;
    public float life, damage;

    public Projectile(Mesh sphere, int tex) {
        useGravity = false;
        textureId = tex;
        flash = 0.5f;                                   // glow
        halfExtents.set(R, R, R);
        addPart(sphere, 0, R, 0, 0.5f, 0.5f, 0.5f, 1f, 0.9f, 0.3f);
    }

    public void fire(Vector3f origin, Vector3f dir, float speed, float damage) {
        transform.setPosition(origin.x, origin.y - R, origin.z);   // feet-origin: centre sits at 'origin'
        velocity.set(dir).mul(speed);
        this.damage = damage;
        life = 2f;
        active = true;
    }

    @Override public void update(float dt) {
        life -= dt;
        transform.translate(velocity.x * dt, velocity.y * dt, velocity.z * dt);
        if (life <= 0f) active = false;
    }
}
