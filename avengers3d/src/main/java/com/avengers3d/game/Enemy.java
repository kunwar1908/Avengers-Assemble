package com.avengers3d.game;

import com.avengers3d.render.Mesh;
import com.avengers3d.scene.GameObject3D;
import org.joml.Vector3f;

/** Chasing enemy. SOLDIER walks on the ground; DRONE hovers (gravity off) and bobs. */
public final class Enemy extends GameObject3D {
    public enum Kind { SOLDIER, DRONE }

    public final Kind kind;
    public float health;
    public boolean alive = true;
    public final float contactDamage;
    public final int scoreValue;

    private final Vector3f target;      // live reference to the player's position (no copying)
    private final float speed;
    private final Vector3f knock = new Vector3f();
    private float t;

    public Enemy(Kind kind, Mesh cube, Mesh sphere, Vector3f target, int wave, int tex) {
        this.kind = kind;
        this.target = target;
        this.textureId = tex;
        float scale = 1f + 0.08f * wave;
        if (kind == Kind.SOLDIER) {
            health = 40f * scale; speed = Math.min(5.5f, 3.2f + 0.12f * wave);
            contactDamage = 10f; scoreValue = 100;
            halfExtents.set(0.4f, 0.95f, 0.3f);
            addPart(cube, -0.17f, 0.45f, 0, 0.28f, 0.9f, 0.3f, 0.2f, 0.2f, 0.25f);   // legs
            addPart(cube,  0.17f, 0.45f, 0, 0.28f, 0.9f, 0.3f, 0.2f, 0.2f, 0.25f);
            addPart(cube, 0, 1.25f, 0, 0.7f, 0.7f, 0.4f, 0.7f, 0.1f, 0.1f);          // torso
            addPart(cube, 0, 1.8f, 0, 0.4f, 0.4f, 0.4f, 0.9f, 0.75f, 0.6f);          // head
        } else {
            useGravity = false;
            health = 25f * scale; speed = 4.5f;
            contactDamage = 8f; scoreValue = 150;
            halfExtents.set(0.5f, 0.5f, 0.5f);
            addPart(sphere, 0, 0.5f, 0, 0.9f, 0.6f, 0.9f, 0.5f, 0.2f, 0.7f);         // body
            addPart(cube, -0.7f, 0.5f, 0, 0.7f, 0.08f, 0.4f, 0.15f, 0.15f, 0.2f);    // wings
            addPart(cube,  0.7f, 0.5f, 0, 0.7f, 0.08f, 0.4f, 0.15f, 0.15f, 0.2f);
            addPart(sphere, 0, 0.5f, 0.4f, 0.2f, 0.2f, 0.2f, 1f, 0.2f, 0.2f);        // eye
        }
    }

    @Override public void update(float dt) {
        t += dt;
        Vector3f p = transform.position();
        float dx = target.x - p.x, dz = target.z - p.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 1e-3f) { dx /= len; dz /= len; transform.setYaw((float) Math.atan2(dx, dz)); }
        else { dx = 0; dz = 0; }

        velocity.x = dx * speed + knock.x;
        velocity.z = dz * speed + knock.z;
        knock.mul(Math.max(0f, 1f - 6f * dt));          // knockback fades out

        if (kind == Kind.DRONE) {                         // P-controller toward a bobbing hover height
            float want = 2.5f + 0.5f * (float) Math.sin(t * 2f);
            velocity.y = (want - p.y) * 3f;
        }
        flash = Math.max(0f, flash - 6f * dt);
    }

    /** Apply damage and push the enemy away from {@code from} on the ground plane. */
    public void damage(float amount, Vector3f from, float knockback) {
        health -= amount;
        flash = 1f;
        Vector3f p = transform.position();
        float dx = p.x - from.x, dz = p.z - from.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 1e-3f) { dx /= len; dz /= len; }
        knock.set(dx * knockback, 0f, dz * knockback);
        if (kind == Kind.SOLDIER && grounded) velocity.y = 3f;
        if (health <= 0f) alive = false;
    }
}
