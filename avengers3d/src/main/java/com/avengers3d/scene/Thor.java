package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** Durable melee hero whose attack calls a short-range lightning strike. */
public final class Thor extends Character3D {
    public Thor(Mesh cube) {
        halfExtents.set(0.7f, 1.25f, 0.7f);
        maxHealth = health = 135f;
        walkSpeed = 7f;
        addPart(cube, 0, 0.65f, 0, 0.75f, 1.2f, 0.55f, 0.2f, 0.35f, 0.8f);
        addPart(cube, 0, 1.9f, 0, 1.0f, 1.2f, 0.65f, 0.25f, 0.35f, 0.9f);
        addPart(cube, 0, 2.85f, 0, 0.55f, 0.55f, 0.55f, 0.75f, 0.75f, 0.85f);
        addPart(cube, -0.85f, 1.9f, 0, 0.3f, 1.2f, 0.35f, 0.2f, 0.35f, 0.85f);
        addPart(cube, 0.85f, 1.9f, 0, 0.3f, 1.2f, 0.35f, 0.2f, 0.35f, 0.85f);
    }

    @Override public String displayName() { return "THOR"; }

    @Override public void update(float dt) {
        tickTimers(dt);
        if (knockTimer <= 0f) {
            velocity.x = moveInput.x * walkSpeed;
            velocity.z = moveInput.z * walkSpeed;
        }
        faceMoveDirection();
    }
}
