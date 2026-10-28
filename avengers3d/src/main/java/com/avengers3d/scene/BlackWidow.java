package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** Fast ranged hero with a rapid dual-shot attack. */
public final class BlackWidow extends Character3D {
    public BlackWidow(Mesh cube) {
        halfExtents.set(0.45f, 0.95f, 0.45f);
        maxHealth = health = 95f;
        walkSpeed = 9f;
        addPart(cube, 0, 0.55f, 0, 0.55f, 1.0f, 0.45f, 0.08f, 0.08f, 0.1f);
        addPart(cube, 0, 1.45f, 0, 0.7f, 0.85f, 0.45f, 0.08f, 0.08f, 0.1f);
        addPart(cube, 0, 2.2f, 0, 0.42f, 0.42f, 0.42f, 0.15f, 0.15f, 0.18f);
        addPart(cube, -0.55f, 1.45f, 0, 0.25f, 0.85f, 0.25f, 0.65f, 0.08f, 0.12f);
        addPart(cube, 0.55f, 1.45f, 0, 0.25f, 0.85f, 0.25f, 0.65f, 0.08f, 0.12f);
    }

    @Override public String displayName() { return "BLACK WIDOW"; }

    @Override public void update(float dt) {
        tickTimers(dt);
        if (knockTimer <= 0f) {
            velocity.x = moveInput.x * walkSpeed;
            velocity.z = moveInput.z * walkSpeed;
        }
        faceMoveDirection();
    }
}
