package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** Flyer: toggling flight disables gravity and switches to thrust + drag steering in full 3D. */
public final class IronMan extends Character3D {
    private static final float FLY_SPEED = 16f, ACCEL = 30f, JUMP_SPEED = 10f;
    private boolean flying;
    private float verticalInput;                 // -1..1 (ascend / descend)

    public IronMan(Mesh cube) {
        halfExtents.set(0.45f, 0.95f, 0.45f);
        maxHealth = health = 100f;
        addPart(cube, -0.2f, 0.45f, 0, 0.32f, 0.9f, 0.34f, 0.95f, 0.8f, 0.2f);    // legs (gold)
        addPart(cube,  0.2f, 0.45f, 0, 0.32f, 0.9f, 0.34f, 0.95f, 0.8f, 0.2f);
        addPart(cube, 0, 1.3f, 0, 0.8f, 0.9f, 0.5f, 0.8f, 0.1f, 0.1f);            // torso (red)
        addPart(cube, 0, 1.4f, 0.26f, 0.22f, 0.22f, 0.05f, 0.6f, 1f, 1f);         // arc reactor
        addPart(cube, -0.6f, 1.3f, 0, 0.28f, 0.85f, 0.3f, 0.8f, 0.1f, 0.1f);      // arms
        addPart(cube,  0.6f, 1.3f, 0, 0.28f, 0.85f, 0.3f, 0.8f, 0.1f, 0.1f);
        addPart(cube, 0, 2.0f, 0, 0.45f, 0.45f, 0.45f, 0.95f, 0.8f, 0.2f);        // helmet (gold)
    }

    @Override public String displayName() { return "IRON MAN"; }
    public boolean isFlying() { return flying; }
    public void setVerticalInput(float v) { verticalInput = v; }

    public void toggleFlight() {
        flying = !flying;
        useGravity = !flying;                    // the whole difference vs Hulk: gravity off in flight
        if (flying) velocity.y = Math.max(velocity.y, 2f);   // small lift-off kick
        else verticalInput = 0f;
    }

    public boolean jump() {
        if (grounded && !flying) { velocity.y = JUMP_SPEED; return true; }
        return false;
    }

    @Override public void update(float dt) {
        tickTimers(dt);
        if (knockTimer <= 0f) {
            if (flying) {
                // Steer velocity toward a target velocity: v += (target - v) * k  (exponential approach).
                float tx = moveInput.x * FLY_SPEED, ty = verticalInput * FLY_SPEED * 0.6f, tz = moveInput.z * FLY_SPEED;
                float k = Math.min(1f, ACCEL * dt / FLY_SPEED);
                velocity.x += (tx - velocity.x) * k;
                velocity.y += (ty - velocity.y) * k;
                velocity.z += (tz - velocity.z) * k;
            } else {
                velocity.x = moveInput.x * 6f;
                velocity.z = moveInput.z * 6f;
            }
        }
        faceMoveDirection();
    }
}
