package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** Ground-bound brawler: gravity ON, double jump along +Y, melee smash, ground pound. */
public final class Hulk extends Character3D {
    private static final float JUMP_SPEED = 14f;
    public boolean poundActive;
    private int airJumps = 1;

    public Hulk(Mesh cube) {
        halfExtents.set(0.9f, 1.6f, 0.9f);        // big hitbox: 1.8 x 3.2 x 1.8 m
        maxHealth = health = 150f;
        walkSpeed = 6f;
        addPart(cube, 0, 0.7f, 0, 1.4f, 1.4f, 0.8f, 0.45f, 0.2f, 0.65f);          // legs (purple shorts)
        addPart(cube, 0, 2.1f, 0, 1.7f, 1.5f, 0.9f, 0.25f, 0.75f, 0.25f);         // torso
        addPart(cube, 0, 3.1f, 0.05f, 0.7f, 0.6f, 0.7f, 0.25f, 0.75f, 0.25f);     // head
        addPart(cube, -1.15f, 2.0f, 0, 0.6f, 1.7f, 0.6f, 0.2f, 0.65f, 0.2f);      // arms
        addPart(cube,  1.15f, 2.0f, 0, 0.6f, 1.7f, 0.6f, 0.2f, 0.65f, 0.2f);
    }

    @Override public String displayName() { return "HULK"; }

    /** Ground jump or one air jump. Apex height h = v^2 / (2|g|) = 14^2 / 50 = 3.9 m. */
    public boolean jump() {
        if (grounded) { velocity.y = JUMP_SPEED; return true; }
        if (airJumps > 0) { airJumps--; velocity.y = JUMP_SPEED; return true; }
        return false;
    }

    public void groundPound() { if (!grounded) { poundActive = true; velocity.y = -40f; } }

    @Override public void update(float dt) {
        tickTimers(dt);
        if (grounded) airJumps = 1;
        if (knockTimer <= 0f) {
            velocity.x = moveInput.x * walkSpeed;
            velocity.z = moveInput.z * walkSpeed;
        }
        faceMoveDirection();
    }
}
