package com.avengers3d.scene;

import org.joml.Vector3f;

/** Shared hero behaviour: movement intent, health, damage cooldown and facing. */
public abstract class Character3D extends GameObject3D {
    /** Desired movement in world space, set each frame by the input layer (length <= 1). */
    public final Vector3f moveInput = new Vector3f();
    public float health = 100f, maxHealth = 100f;
    public float invuln;       // seconds of damage immunity after a hit
    public float cooldown;     // seconds until the next attack
    public float specialCooldown; // seconds until the special power is ready
    public float knockTimer;   // while > 0 the input layer may not override horizontal velocity
    protected float walkSpeed = 6f;

    public abstract String displayName();

    /** @return true if damage was actually applied (not immune, not already dead). */
    public boolean damage(float amount) {
        if (invuln > 0f || health <= 0f) return false;
        health = Math.max(0f, health - amount);
        invuln = 0.8f;
        knockTimer = 0.25f;
        return true;
    }

    public void heal(float amount) { health = Math.min(maxHealth, health + amount); }

    protected void tickTimers(float dt) {
        invuln = Math.max(0f, invuln - dt);
        cooldown = Math.max(0f, cooldown - dt);
        specialCooldown = Math.max(0f, specialCooldown - dt);
        knockTimer = Math.max(0f, knockTimer - dt);
        flash = (invuln > 0f && ((int) (invuln * 16f) & 1) == 0) ? 0.6f : 0f;   // blink while immune
    }

    protected void faceMoveDirection() {
        if (moveInput.x * moveInput.x + moveInput.z * moveInput.z > 1e-4f)
            transform.setYaw((float) Math.atan2(moveInput.x, moveInput.z));    // heading about +Y
    }

    public void faceYaw(float yaw) { transform.setYaw(yaw); }
}
