package com.avengers3d.scene;

import org.joml.Vector3f;

/** Shared state for heroes: movement intent, health, timers. Concrete behaviour lives in game.Hero. */
public abstract class Character3D extends GameObject3D {
    public final Vector3f moveInput = new Vector3f();
    public float health = 100f, maxHealth = 100f;
    public float invuln;          // post-hit grace (blinks)
    public float iFrames;         // dodge/dash immunity (no blink)
    public float knockTimer;      // while > 0 the player cannot steer horizontally
    public float cooldown, specialCooldown, dodgeCooldown;
    public boolean dead;

    public abstract String displayName();

    /** @return true if damage was applied. */
    public boolean damage(float amount) {
        if (dead || invuln > 0f || iFrames > 0f) return false;
        health = Math.max(0f, health - amount);
        invuln = 0.7f;
        knockTimer = 0.2f;
        if (health <= 0f) dead = true;
        return true;
    }

    public void heal(float amount) { health = Math.min(maxHealth, health + amount); }

    protected void tickTimers(float dt) {
        invuln = Math.max(0f, invuln - dt);
        iFrames = Math.max(0f, iFrames - dt);
        knockTimer = Math.max(0f, knockTimer - dt);
        cooldown = Math.max(0f, cooldown - dt);
        specialCooldown = Math.max(0f, specialCooldown - dt);
        dodgeCooldown = Math.max(0f, dodgeCooldown - dt);
        flash = (invuln > 0f && ((int) (invuln * 18f) & 1) == 0) ? 0.5f : 0f;
    }

    /** Turn {@code cur} toward {@code target} along the shortest arc by at most {@code maxDelta} radians. */
    public static float approachAngle(float cur, float target, float maxDelta) {
        float d = (float) Math.atan2(Math.sin(target - cur), Math.cos(target - cur));
        if (Math.abs(d) <= maxDelta) return target;
        return cur + Math.signum(d) * maxDelta;
    }
}
