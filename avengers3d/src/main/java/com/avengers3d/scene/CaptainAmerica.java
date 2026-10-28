package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** Agile ground hero whose attack launches a high-damage shield projectile. */
public final class CaptainAmerica extends Character3D {
    public CaptainAmerica(Mesh cube) {
        halfExtents.set(0.55f, 1.1f, 0.55f);
        maxHealth = health = 120f;
        walkSpeed = 8f;
        addPart(cube, 0, 0.65f, 0, 0.65f, 1.2f, 0.5f, 0.15f, 0.2f, 0.75f);
        addPart(cube, 0, 1.75f, 0, 0.85f, 1.0f, 0.55f, 0.12f, 0.35f, 0.9f);
        addPart(cube, 0, 2.6f, 0, 0.5f, 0.5f, 0.5f, 0.85f, 0.85f, 0.9f);
        addPart(cube, -0.62f, 1.75f, 0, 0.25f, 0.9f, 0.3f, 0.12f, 0.3f, 0.85f);
        addPart(cube, 0.62f, 1.75f, 0, 0.25f, 0.9f, 0.3f, 0.12f, 0.3f, 0.85f);
        addPart(cube, 0, 1.75f, 0.38f, 0.42f, 0.42f, 0.08f, 0.9f, 0.1f, 0.1f);
    }

    @Override public String displayName() { return "CAPTAIN AMERICA"; }

    @Override public void update(float dt) {
        tickTimers(dt);
        if (knockTimer <= 0f) {
            velocity.x = moveInput.x * walkSpeed;
            velocity.z = moveInput.z * walkSpeed;
        }
        faceMoveDirection();
    }
}
