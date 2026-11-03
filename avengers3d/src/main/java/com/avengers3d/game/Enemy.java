package com.avengers3d.game;

import com.avengers3d.audio.AudioManager.Sound;
import com.avengers3d.scene.Character3D;
import com.avengers3d.scene.GameObject3D;
import com.avengers3d.scene.Look;
import com.avengers3d.scene.Meshes;
import com.avengers3d.scene.Rig;
import org.joml.Vector3f;

import java.util.Random;

import static com.avengers3d.scene.Look.c;

/**
 * All enemy types share one class; behaviour is a small state machine per kind.
 * Regular enemies telegraph their attacks (glow + pose) so the player can react; bosses run attack patterns.
 */
public final class Enemy extends GameObject3D {
    public enum Kind { TROOPER, DRONE, BRUTE, VEX, OMEGA }

    public final Kind kind;
    public final String name;
    public final boolean boss;
    public final int score;
    public final float barHeight;
    public float health, maxHealth, stun;
    public boolean alive = true;

    private final Rig rig;
    private final Rig.Input rin = new Rig.Input();
    private Part ring, eye;
    private final Vector3f knock = new Vector3f(), lockDir = new Vector3f(), lockPos = new Vector3f(), tmp = new Vector3f(), tmp2 = new Vector3f();
    private final Random rnd = new Random();
    private final int wave;
    private float speed, t, attackTimer, stateT, strafeT, yaw, orbit, hit, kick, laserTick, lockAngle;
    private int state, strafe = 1, phase = 1;
    private boolean summoned1, summoned2;
    private float moveX, moveZ, moveY, faceX, faceZ;
    private boolean driveY;

    public Enemy(Kind kind, Meshes m, int wave, int tex) {
        this.kind = kind;
        this.wave = wave;
        this.textureId = tex;
        attackTimer = 1f + rnd.nextFloat() * 2f;
        strafe = rnd.nextBoolean() ? 1 : -1;
        float scale = 1f + 0.07f * wave;
        Look l = new Look();
        switch (kind) {
            case TROOPER -> {
                name = "UMBRA TROOPER"; boss = false; score = 100; speed = 4.2f; barHeight = 2.2f;
                maxHealth = 45f * scale;
                l.suit = c(.18f, .19f, .22f); l.suit2 = c(.14f, .15f, .18f); l.skin = c(.10f, .10f, .13f); l.helm = c(.20f, .20f, .25f);
                l.boot = c(.07f, .07f, .09f); l.glove = c(.1f, .1f, .12f); l.eyes = true; l.accent = c(1f, .2f, .15f); l.prop = Look.Prop.PISTOLS;
                halfExtents.set(.38f, .93f, .32f);
            }
            case BRUTE -> {
                name = "UMBRA BRUTE"; boss = false; score = 300; speed = 3.2f; barHeight = 3.3f;
                maxHealth = 230f * scale;
                l.scale = 1.35f; l.bulk = 1.6f; l.suit = c(.30f, .14f, .12f); l.suit2 = c(.20f, .12f, .10f); l.skin = c(.18f, .16f, .15f);
                l.helm = c(.28f, .10f, .10f); l.pad = c(.36f, .36f, .42f); l.spikes = true; l.eyes = true; l.accent = c(1f, .45f, .1f);
                l.boot = c(.1f, .08f, .08f);
                halfExtents.set(.72f, 1.25f, .55f);
            }
            case VEX -> {
                name = "COMMANDER VEX"; boss = true; score = 2000; speed = 5.4f; barHeight = 3.6f;
                maxHealth = 900f;
                l.scale = 1.4f; l.bulk = 1.25f; l.suit = c(.12f, .10f, .22f); l.suit2 = c(.10f, .08f, .18f); l.skin = c(.14f, .12f, .20f);
                l.helm = c(.20f, .12f, .35f); l.cape = c(.25f, .05f, .40f); l.pad = c(.40f, .30f, .50f); l.eyes = true; l.accent = c(.3f, .9f, 1f);
                l.prop = Look.Prop.BLADE; l.boot = c(.08f, .06f, .14f);
                halfExtents.set(.6f, 1.3f, .5f);
            }
            case OMEGA -> {
                name = "WARDEN OMEGA"; boss = true; score = 5000; speed = 2.3f; barHeight = 8.2f;
                maxHealth = 2600f;
                l.scale = 3.4f; l.bulk = 1.5f; l.suit = c(.20f, .20f, .26f); l.suit2 = c(.14f, .14f, .18f); l.skin = c(.08f, .08f, .10f);
                l.helm = c(.30f, .30f, .38f); l.pad = c(.45f, .10f, .10f); l.spikes = true; l.eyes = true; l.chestGlow = true; l.cape = c(.18f, .04f, .10f);
                l.accent = c(1f, .25f, .9f); l.prop = Look.Prop.CANNONS; l.boot = c(.1f, .1f, .13f);
                halfExtents.set(1.9f, 3.0f, 1.5f);
            }
            default -> {   // DRONE
                name = "UMBRA DRONE"; boss = false; score = 150; speed = 4.5f; barHeight = 1.5f;
                maxHealth = 28f * scale;
                useGravity = false;
                halfExtents.set(.45f, .4f, .45f);
            }
        }
        health = maxHealth;
        if (kind == Kind.DRONE) {
            rig = null;
            addPart(m.sphere(), null, 0, .4f, 0, 0, 0, 0, .8f, .55f, .8f, .30f, .22f, .42f).spec = .6f;
            ring = addBox(m.cube(), 0, .4f, 0, 1.4f, .05f, .14f, .55f, .2f, .8f);
            ring.emissive = 1.5f;
            addBox(m.cube(), 0, .4f, 0, .14f, .05f, 1.4f, .55f, .2f, .8f).emissive = 1.5f;
            eye = addPart(m.sphere(), null, 0, .42f, .34f, 0, 0, 0, .22f, .22f, .22f, 1f, .15f, .15f);
            eye.emissive = 3f;
        } else {
            rig = new Rig(this, m, l);
            rim = .3f;
        }
    }

    @Override public void update(float dt) { }

    public float contactDamage() {
        return switch (kind) {
            case TROOPER -> 3f;
            case DRONE -> state == 2 ? 14f : 0f;
            case BRUTE -> 8f;
            case VEX -> state == 2 ? 26f : 8f;
            case OMEGA -> 16f;
        };
    }

    public float yaw() { return yaw; }

    /** Apply damage and a knockback push (bosses barely move). */
    public void hurt(float amount, Vector3f from, float knockback) {
        health -= amount;
        flash = 1f; hit = 1f;
        if (boss) knockback *= 0.12f;
        Vector3f p = transform.position();
        float dx = p.x - from.x, dz = p.z - from.z, len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len > 1e-3f) { dx /= len; dz /= len; }
        knock.set(dx * knockback, 0f, dz * knockback);
        if (!boss && kind != Kind.DRONE && grounded && knockback > 1f) velocity.y = 3f;
        if (health <= 0f) alive = false;
    }

    public void stun(float seconds) { if (!boss) stun = Math.max(stun, seconds); }

    private void glow(float mult) {
        if (rig != null) rig.setGlow(mult);
        if (eye != null) eye.emissive = 3f * mult;
    }

    // ---- AI ----------------------------------------------------------------
    public void tick(World w, float dt) {
        t += dt;
        flash = Math.max(0f, flash - 6f * dt);
        hit = Math.max(0f, hit - 4f * dt);
        kick = Math.max(0f, kick - dt);
        Hero p = w.player;
        if (p == null) return;
        Vector3f pos = transform.position(), pp = p.transform().position();
        float dx = pp.x - pos.x, dz = pp.z - pos.z;
        float dist = Math.max(0.001f, (float) Math.sqrt(dx * dx + dz * dz));
        faceX = dx / dist; faceZ = dz / dist;
        moveX = moveZ = moveY = 0f; driveY = false;
        rin.attack = Rig.ATK_NONE; rin.attackT = 0f;
        knock.mul(Math.max(0f, 1f - 6f * dt));

        if (stun > 0f) {
            stun -= dt;
            glow(1f);
            if (rnd.nextFloat() < dt * 14f) w.fx.sparks(pos.x, pos.y + 1.2f, pos.z, .6f, 1.6f, 3f, 3, 4f);
            if (kind == Kind.DRONE) { moveY = -2f; driveY = true; }
        } else {
            switch (kind) {
                case TROOPER -> trooper(w, dt, dist, pp);
                case DRONE -> drone(w, dt, dist, pp);
                case BRUTE -> brute(w, dt, dist);
                case VEX -> vex(w, dt, dist, pp);
                case OMEGA -> omega(w, dt, dist, pp);
            }
        }
        velocity.x = moveX + knock.x;
        velocity.z = moveZ + knock.z;
        if (driveY) velocity.y = moveY;

        float target = (float) Math.atan2(faceX, faceZ);
        yaw = Character3D.approachAngle(yaw, target, (boss ? 4f : 9f) * dt);
        if (kind == Kind.DRONE) {
            float tilt = Math.min(.5f, (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) / 40f);
            transform.setEuler(tilt, yaw, 0f);
            ring.rotY += dt * 14f;
        } else {
            transform.setYaw(yaw);
            rin.speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            rin.maxSpeed = Math.max(1f, speed);
            rin.grounded = grounded; rin.vy = velocity.y; rin.hit = hit;
            rig.animate(rin, dt);
        }
    }

    private void trooper(World w, float dt, float dist, Vector3f pp) {
        attackTimer -= dt;
        if (state == 0) {
            strafeT -= dt;
            if (strafeT <= 0f) { strafe = -strafe; strafeT = 1.5f + rnd.nextFloat() * 2f; }
            if (dist > 14f)      { moveX = faceX * speed; moveZ = faceZ * speed; }
            else if (dist < 8f)  { moveX = -faceX * speed * .8f; moveZ = -faceZ * speed * .8f; }
            else                 { moveX = -faceZ * strafe * speed * .6f; moveZ = faceX * strafe * speed * .6f; }
            if (attackTimer <= 0f && dist < 30f) { state = 1; stateT = 0f; }
        } else {                                              // aiming: glows red, then fires
            stateT += dt;
            glow(1f + stateT * 4f);
            rin.attack = Rig.ATK_SHOOT; rin.attackT = .2f;
            if (stateT >= .55f) {
                Vector3f pos = transform.position();
                tmp.set(pos.x + faceX * .6f, pos.y + 1.45f, pos.z + faceZ * .6f);
                tmp2.set(pp.x + (rnd.nextFloat() - .5f) * 1.4f, pp.y + 1.1f, pp.z + (rnd.nextFloat() - .5f) * 1.4f);
                w.fireEnemyShot(tmp, tmp2, 21f, 8f * (1f + .04f * wave), Projectile.Style.ENEMY);
                w.audio.play(Sound.ENEMY_SHOOT, pos);
                state = 0; attackTimer = 1.5f + rnd.nextFloat() * 1.5f; kick = .25f; glow(1f);
            }
        }
        if (kick > 0f && state == 0) { rin.attack = Rig.ATK_SHOOT; rin.attackT = .05f; }
    }

    private void drone(World w, float dt, float dist, Vector3f pp) {
        attackTimer -= dt;
        Vector3f pos = transform.position();
        float wantY = 2.6f + .5f * (float) Math.sin(t * 2f);
        driveY = true;
        if (state == 0) {
            orbit += dt * .8f * strafe;
            float ex = pp.x + (float) Math.cos(orbit) * 7f - pos.x, ez = pp.z + (float) Math.sin(orbit) * 7f - pos.z;
            float el = (float) Math.sqrt(ex * ex + ez * ez);
            if (el > .01f) { float k = Math.min(1f, el / 2.5f) * speed; moveX = ex / el * k; moveZ = ez / el * k; }
            if (attackTimer <= 0f && dist < 28f) {
                state = 1; stateT = 0f; lockPos.set(pp.x, pp.y + 1f, pp.z);
                w.audio.play(Sound.WARNING, pos);
            }
        } else if (state == 1) {                              // telegraph: hang still and glow
            stateT += dt; glow(1f + stateT * 5f);
            wantY = pos.y;
            if (stateT >= .6f) {
                state = 2; stateT = 0f;
                lockDir.set(lockPos).sub(pos.x, pos.y + .4f, pos.z).normalize();
            }
        } else {                                              // dive
            stateT += dt;
            moveX = lockDir.x * 21f; moveZ = lockDir.z * 21f; moveY = lockDir.y * 21f;
            w.fx.trail(pos.x, pos.y + .4f, pos.z, 3f, .4f, .3f, .5f);
            if (stateT >= .7f) { state = 0; attackTimer = 3f + rnd.nextFloat() * 2f; glow(1f); }
            return;
        }
        moveY = (wantY - pos.y) * 3f;
    }

    private void brute(World w, float dt, float dist) {
        attackTimer -= dt;
        Vector3f pos = transform.position();
        if (state == 0) {
            if (dist > 3.2f) { moveX = faceX * speed; moveZ = faceZ * speed; }
            if (dist < 4.4f && attackTimer <= 0f) { state = 1; stateT = 0f; }
        } else if (state == 1) {                              // wind-up, arm raised overhead
            stateT += dt; glow(1f + stateT * 3f);
            rin.attack = Rig.ATK_SWING; rin.attackT = Math.min(.38f, stateT / .75f * .38f);
            w.fx.groundWarning(pos.x + faceX * 2.4f, pos.z + faceZ * 2.4f, 4.2f, 3f, .5f, .2f);
            if (stateT >= .75f) {
                w.enemySlam(pos.x + faceX * 2.4f, pos.z + faceZ * 2.4f, 4.2f, 22f * (1f + .03f * wave), 14f, false);
                w.fx.dust(pos.x + faceX * 2.4f, pos.y, pos.z + faceZ * 2.4f, 4f, 14);
                w.audio.play(Sound.SLAM, pos);
                w.addShake(.5f);
                state = 2; stateT = 0f; attackTimer = 2.2f; glow(1f);
            }
        } else {
            stateT += dt;
            rin.attack = Rig.ATK_SWING; rin.attackT = .45f + Math.min(.5f, stateT / .6f * .5f);
            if (stateT >= .6f) state = 0;
        }
    }

    private void vex(World w, float dt, float dist, Vector3f pp) {
        attackTimer -= dt;
        Vector3f pos = transform.position();
        if (health < maxHealth * .5f && !summoned1) {
            summoned1 = true; speed *= 1.25f;
            for (int i = 0; i < 2; i++) w.spawnEnemy(Kind.TROOPER, pos.x + (i == 0 ? -4f : 4f), pos.z + 3f);
            w.banner("VEX CALLS REINFORCEMENTS!");
            w.audio.play(Sound.ROAR, pos);
        }
        switch (state) {
            case 0 -> {
                if (dist > 2.5f) { moveX = faceX * speed; moveZ = faceZ * speed; }
                if (attackTimer <= 0f) { state = dist > 8f ? 1 : 4; stateT = 0f; }
            }
            case 1 -> {                                       // charge telegraph: red lane previews the dash
                stateT += dt; glow(1f + stateT * 3f);
                if (stateT < .6f) lockDir.set(faceX, 0f, faceZ);
                w.fx.line(pos.x, .3f, pos.z, pos.x + lockDir.x * 16f, .3f, pos.z + lockDir.z * 16f, .8f, 3f, .3f, .2f, .35f, .08f);
                rin.attack = Rig.ATK_SWING; rin.attackT = .2f;
                if (stateT >= .9f) { state = 2; stateT = 0f; w.audio.play(Sound.ROAR, pos); }
            }
            case 2 -> {                                       // the charge itself
                stateT += dt;
                moveX = lockDir.x * 26f; moveZ = lockDir.z * 26f;
                rin.attack = Rig.ATK_DASH; rin.attackT = .5f;
                w.fx.trail(pos.x, pos.y + 1.2f, pos.z, .5f, 1.6f, 3f, .7f);
                if (stateT >= .65f) { state = 3; stateT = 0f; glow(1f); }
            }
            case 3 -> { stateT += dt; if (stateT >= .9f) { state = 0; attackTimer = 1.1f; } }   // exposed after a charge
            case 4 -> {                                       // blade wind-up then sweeping strike
                stateT += dt; glow(1f + stateT * 3f);
                rin.attack = Rig.ATK_SWING; rin.attackT = Math.min(.38f, stateT / .5f * .38f);
                w.fx.groundWarning(pos.x + faceX * 2.4f, pos.z + faceZ * 2.4f, 4.8f, .3f, .9f, 3f);
                if (stateT >= .5f) {
                    w.enemySlam(pos.x + faceX * 2.4f, pos.z + faceZ * 2.4f, 4.8f, 22f, 12f, false);
                    w.fx.ring(pos.x + faceX * 2.4f, pos.y, pos.z + faceZ * 2.4f, 4.8f, .3f, 1.6f, 3f);
                    w.audio.play(Sound.SLAM, pos); w.addShake(.4f);
                    state = 5; stateT = 0f; glow(1f);
                }
            }
            default -> {
                stateT += dt;
                rin.attack = Rig.ATK_SWING; rin.attackT = .5f + Math.min(.45f, stateT / .5f * .45f);
                if (stateT >= .5f) { state = 0; attackTimer = 1.5f; }
            }
        }
    }

    private void omega(World w, float dt, float dist, Vector3f pp) {
        attackTimer -= dt;
        Vector3f pos = transform.position();
        int np = health > maxHealth * .66f ? 1 : (health > maxHealth * .33f ? 2 : 3);
        if (np != phase) {
            phase = np;
            w.banner(phase == 2 ? "OMEGA: SYSTEMS OVERCLOCKED" : "OMEGA: CRITICAL - FINAL PROTOCOL");
            w.addShake(1.2f); w.audio.play(Sound.BOSS, pos); w.fx.ring(pos.x, pos.y, pos.z, 12f, 3f, .4f, 2.5f);
        }
        switch (state) {
            case 0 -> {
                float sp = speed + .5f * (phase - 1);
                if (dist > 10f) { moveX = faceX * sp; moveZ = faceZ * sp; }
                if (attackTimer <= 0f) {
                    float r = rnd.nextFloat();
                    int next = phase == 1 ? (r < .6f ? 1 : 3) : (r < .35f ? 1 : r < .6f ? 3 : r < .85f ? 4 : 5);
                    if (next == 5) {
                        if ((phase == 2 && summoned1) || (phase == 3 && summoned2)) next = 1;
                        else if (phase == 2) summoned1 = true; else summoned2 = true;
                    }
                    state = next; stateT = 0f;
                    lockPos.set(pp.x, 0f, pp.z);
                    lockAngle = (float) Math.atan2(faceX, faceZ);
                    laserTick = 0f;
                }
            }
            case 1 -> {                                       // SLAM: marked circle, jump or leave it
                stateT += dt;
                glow(1f + stateT * 1.5f);
                w.fx.groundWarning(lockPos.x, lockPos.z, 7.5f, 3f, .3f, .2f);
                rin.attack = Rig.ATK_SLAM; rin.attackT = Math.min(.44f, stateT / 1.1f * .44f);
                if (stateT >= 1.1f) {
                    w.enemySlam(lockPos.x, lockPos.z, 7.5f, 32f, 16f, true);
                    w.fx.ring(lockPos.x, 0f, lockPos.z, 7.5f, 3f, .5f, 2.5f);
                    w.fx.explosion(lockPos.x, .6f, lockPos.z, 2.2f);
                    w.fx.dust(lockPos.x, 0f, lockPos.z, 7f, 22);
                    w.audio.play(Sound.SLAM, lockPos); w.addShake(1.3f);
                    state = 2; stateT = 0f; glow(1f);
                }
            }
            case 2 -> {
                stateT += dt;
                rin.attack = Rig.ATK_SLAM; rin.attackT = .45f + Math.min(.5f, stateT / .9f * .5f);
                if (stateT >= .9f) { state = 0; attackTimer = 2.2f - .4f * (phase - 1); }
            }
            case 3 -> {                                       // MISSILES: fan of slow shots
                stateT += dt; glow(1f + stateT * 2f);
                rin.attack = Rig.ATK_CAST; rin.attackT = Math.min(.5f, stateT / .9f * .5f);
                if (stateT >= .9f) {
                    float base = (float) Math.atan2(faceX, faceZ);
                    for (int i = -3; i <= 3; i++) {
                        float a = base + i * .17f;
                        tmp.set(pos.x + (float) Math.sin(a) * 3f, pos.y + 5f, pos.z + (float) Math.cos(a) * 3f);
                        tmp2.set(tmp.x + (float) Math.sin(a) * 30f, 1.2f, tmp.z + (float) Math.cos(a) * 30f);
                        w.fireEnemyShot(tmp, tmp2, 15f, 10f, Projectile.Style.MISSILE);
                    }
                    w.audio.play(Sound.ENEMY_SHOOT, pos); w.addShake(.5f);
                    state = 2; stateT = 0f; glow(1f);
                }
            }
            case 4 -> {                                       // LASER: telegraph, then a sweeping beam (jump over it)
                stateT += dt;
                float a = lockAngle;
                boolean sweeping = stateT >= 1.3f;
                if (sweeping) a = lockAngle - .8f + 1.6f * Math.min(1f, (stateT - 1.3f) / 2.4f);
                float ddx = (float) Math.sin(a), ddz = (float) Math.cos(a);
                float ox = pos.x + ddx * 3.2f, oz = pos.z + ddz * 3.2f;
                faceX = ddx; faceZ = ddz;
                rin.attack = Rig.ATK_CAST; rin.attackT = .45f;
                glow(sweeping ? 2f : 1f + stateT);
                if (!sweeping) w.fx.line(ox, 1.1f, oz, ox + ddx * 42f, 1.1f, oz + ddz * 42f, 1.4f, 3f, .3f, .3f, .22f, .06f);
                else {
                    w.fx.line(ox, 1.1f, oz, ox + ddx * 42f, 1.1f, oz + ddz * 42f, .7f, 3.2f, .4f, 2.8f, .65f, .09f);
                    laserTick -= dt;
                    if (laserTick <= 0f) {
                        laserTick = .2f;
                        float rx = pp.x - ox, rz = pp.z - oz;
                        float along = rx * ddx + rz * ddz, perp = rx * ddz - rz * ddx;
                        if (along > 0f && along < 44f && Math.abs(perp) < 1.5f && pp.y < 2.2f)
                            w.damagePlayer(14f, tmp.set(ox, 0f, oz), 6f);
                    }
                }
                if (stateT >= 3.7f) { state = 2; stateT = 0f; glow(1f); attackTimer = 1.4f; }
            }
            default -> {                                      // SUMMON
                stateT += dt; glow(1f + stateT);
                rin.attack = Rig.ATK_CAST; rin.attackT = Math.min(.5f, stateT / 1.0f * .5f);
                if (stateT >= 1f) {
                    for (int i = 0; i < 3; i++) {
                        float a = i * 2.094f + rnd.nextFloat();
                        w.spawnEnemy(Kind.DRONE, pos.x + (float) Math.cos(a) * 9f, pos.z + (float) Math.sin(a) * 9f);
                    }
                    w.banner("OMEGA DEPLOYS DRONES");
                    w.audio.play(Sound.ROAR, pos);
                    state = 2; stateT = 0f; glow(1f);
                }
            }
        }
    }
}
