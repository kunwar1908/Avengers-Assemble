package com.avengers3d.game;

import com.avengers3d.render.Camera3D;
import com.avengers3d.scene.Character3D;
import com.avengers3d.scene.Meshes;
import com.avengers3d.scene.Rig;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** The player's character: one class for every hero, driven by a HeroDef. */
public final class Hero extends Character3D {
    public final HeroDef def;
    public final Rig rig;
    private final Rig.Input rin = new Rig.Input();

    public boolean flying, poundActive;
    public float rageTimer, dashTimer, dashSpeed, dashDamage, aimTimer, aimYaw, hitRecoil, deathT, time;
    public int fireSide = 1;
    public final Vector3f dashDir = new Vector3f();
    public final List<Enemy> dashHits = new ArrayList<>();

    private final Vector3f tmpV = new Vector3f();
    private int atkStyle;
    private float atkT, atkDur, yaw, lean, verticalInput;
    private int airJumpsLeft;

    public Hero(HeroDef def, Meshes meshes) {
        this.def = def;
        float s = def.look.scale, w = def.look.bulk;
        float hx = Math.max(.30f, .27f * w * s + .08f);
        halfExtents.set(hx, .93f * s, hx * .85f);
        maxHealth = health = def.health;
        rim = .7f;
        rig = new Rig(this, meshes, def.look);
    }

    @Override public String displayName() { return def.name; }
    public float damageMult() { return rageTimer > 0f ? 1.8f : 1f; }
    public float yaw() { return yaw; }
    public void setYaw(float y) { yaw = y; }
    public float stepHeight() { return def.look.scale; }

    @Override public boolean damage(float amount) {
        boolean hit = super.damage(amount);
        if (hit) hitRecoil = 1f;
        return hit;
    }

    /** Heading (facing angle about +Y, 0 = +Z) of the camera's ground-plane forward direction. */
    private float aimOf(Camera3D cam) {
        cam.flatForward(tmpV);
        return (float) Math.atan2(tmpV.x, tmpV.z);
    }

    // ---- actions ----------------------------------------------------------
    public boolean jump() {
        if (dead || flying) return false;
        if (grounded) { velocity.y = def.jumpSpeed; return true; }
        if (airJumpsLeft > 0) { airJumpsLeft--; velocity.y = def.jumpSpeed; return true; }
        return false;
    }

    public boolean toggleFlight() {
        if (!def.canFly || dead) return false;
        flying = !flying;
        useGravity = !flying;
        verticalInput = 0f;
        if (flying) velocity.y = Math.max(velocity.y, 3f);
        return true;
    }

    public void setVerticalInput(float v) { verticalInput = v; }

    public void groundPound() {
        if (def.groundPound && !grounded && !flying && !dead) { poundActive = true; velocity.y = -42f; }
    }

    public void playAnim(int style, float duration) { atkStyle = style; atkT = 0f; atkDur = Math.max(.05f, duration); }

    /** Burst of horizontal speed along {@code dir} (Y ignored). */
    public void startDash(Vector3f dir, float speed, float seconds) {
        float l = (float) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        if (l < 1e-4f) dashDir.set((float) Math.sin(yaw), 0, (float) Math.cos(yaw));
        else dashDir.set(dir.x / l, 0, dir.z / l);
        dashSpeed = speed; dashTimer = seconds;
        dashHits.clear();
        playAnim(Rig.ATK_DASH, seconds);
    }

    public boolean tryAttack(World w, Camera3D cam) {
        if (dead || cooldown > 0f) return false;
        cooldown = def.attackCd;
        aimYaw = aimOf(cam); aimTimer = .45f;
        playAnim(def.attackAnim, def.attackTime);
        def.attack.use(w, this, cam);
        return true;
    }

    public boolean trySpecial(World w, Camera3D cam) {
        if (dead || specialCooldown > 0f) return false;
        specialCooldown = def.specialCd;
        aimYaw = aimOf(cam); aimTimer = .6f;
        playAnim(def.specialAnim, def.specialTime);
        def.special.use(w, this, cam);
        return true;
    }

    public boolean tryDodge(Vector3f dir) {
        if (dead || flying || dodgeCooldown > 0f || dashTimer > 0f) return false;
        dodgeCooldown = 1.1f;
        iFrames = .32f;
        startDash(dir, 17f, .22f);
        return true;
    }

    // ---- simulation -------------------------------------------------------
    @Override public void update(float dt) {
        time += dt;
        tickTimers(dt);
        rageTimer = Math.max(0f, rageTimer - dt);
        hitRecoil = Math.max(0f, hitRecoil - dt * 4f);
        aimTimer = Math.max(0f, aimTimer - dt);

        if (dead) {
            deathT += dt;
            velocity.x = velocity.z = 0f;
            transform.setEuler(-Math.min(1.45f, deathT * 3.2f), yaw, 0f);   // topple backwards
            return;
        }
        if (atkDur > 0f) { atkT += dt / atkDur; if (atkT >= 1f) { atkStyle = Rig.ATK_NONE; atkDur = 0f; atkT = 0f; } }

        boolean moving = moveInput.x * moveInput.x + moveInput.z * moveInput.z > 1e-3f;
        if (dashTimer > 0f) {
            dashTimer -= dt;
            velocity.x = dashDir.x * dashSpeed;
            velocity.z = dashDir.z * dashSpeed;
            if (dashTimer <= 0f) { dashSpeed = 0f; dashDamage = 0f; if (atkStyle == Rig.ATK_DASH) { atkStyle = 0; atkDur = 0f; } }
        } else if (knockTimer <= 0f) {
            float spd = def.walkSpeed * (rageTimer > 0f ? 1.25f : 1f);
            if (flying) {
                float fs = 17f, k = Math.min(1f, 30f * dt / fs);
                velocity.x += (moveInput.x * fs - velocity.x) * k;
                velocity.y += (verticalInput * fs * .6f - velocity.y) * k;
                velocity.z += (moveInput.z * fs - velocity.z) * k;
            } else {
                velocity.x = moveInput.x * spd;
                velocity.z = moveInput.z * spd;
            }
        }
        if (grounded) airJumpsLeft = def.airJumps;

        float target = yaw;
        if (dashTimer > 0f) target = (float) Math.atan2(dashDir.x, dashDir.z);
        else if (aimTimer > 0f) target = aimYaw;
        else if (moving) target = (float) Math.atan2(moveInput.x, moveInput.z);
        yaw = approachAngle(yaw, target, 18f * dt);

        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float tgtLean = flying ? Math.min(.95f, speed / 17f * 1.0f) : 0f;      // pitch forward when flying fast
        lean += (tgtLean - lean) * (1f - (float) Math.exp(-8f * dt));
        transform.setEuler(lean, yaw, 0f);
    }

    /** Pose the skeleton from the final (post-physics) state. */
    public void animate(float dt) {
        rin.speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        rin.maxSpeed = def.walkSpeed;
        rin.vy = velocity.y;
        rin.grounded = grounded;
        rin.flying = flying;
        rin.hit = hitRecoil;
        rin.attack = atkStyle;
        rin.attackT = atkT;
        rig.animate(rin, dt);
        float rage = rageTimer > 0f ? .7f + .3f * (float) Math.sin(time * 12f) : 0f;   // pulsing rage glow
        rig.chest.emissive = rage; rig.head.emissive = rage * .6f;
        rig.shL.emissive = rage * .6f; rig.shR.emissive = rage * .6f;
    }
}
