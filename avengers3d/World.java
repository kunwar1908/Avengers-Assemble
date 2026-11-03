package com.avengers3d.game;

import com.avengers3d.audio.AudioManager;
import com.avengers3d.audio.AudioManager.Sound;
import com.avengers3d.game.Level.WaveSpec;
import com.avengers3d.physics.AABB;
import com.avengers3d.physics.BoundingSphere;
import com.avengers3d.physics.Physics3D;
import com.avengers3d.render.Camera3D;
import com.avengers3d.render.Lights;
import com.avengers3d.render.Mesh;
import com.avengers3d.render.ParticleSystem;
import com.avengers3d.scene.GameObject3D;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Game rules and simulation: level geometry, waves, combat, scoring. The GameApp owns windows, menus and states. */
public final class World {
    public final Assets assets;
    public final Fx fx;
    public final AudioManager audio;
    public final DamageNumbers numbers = new DamageNumbers();
    public final Lights lights = new Lights();

    public Level level;
    public boolean storyMode, showcase, levelComplete;
    public Hero player;
    public Enemy boss;
    public final List<Enemy> enemies = new ArrayList<>();
    public int score, kills, wave, combo;
    public float comboTimer, bannerTimer, elapsed, deathTimer, completeTimer, damageFlash, healFlash;
    public String bannerText = "";
    public float pendingShake, pendingHitStop;          // consumed by GameApp each frame

    private final List<Block> solids = new ArrayList<>(), props = new ArrayList<>();
    private final Projectile[] pool = new Projectile[96];
    private final List<GameObject3D> drawList = new ArrayList<>(512);
    private Block ground;
    private final AABB arena = new AABB(), beamBox = new AABB();
    private boolean waveActive;
    private float waveTimer, flashT, flashX, flashY, flashZ, flashR, flashG, flashB;
    private final Random rnd = new Random();
    private final BoundingSphere probe = new BoundingSphere();
    private final Vector3f mtv = new Vector3f(), fwd = new Vector3f(), rgt = new Vector3f(), muzzle = new Vector3f(),
                           aim = new Vector3f(), dir = new Vector3f(), tmp = new Vector3f();

    public World(Assets assets, ParticleSystem particles, AudioManager audio) {
        this.assets = assets; this.audio = audio; this.fx = new Fx(particles);
        for (int i = 0; i < pool.length; i++) pool[i] = new Projectile(assets.meshes);
    }

    // ---- level building ---------------------------------------------------
    public Block solid(Mesh m, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        Block blk = new Block(m, x, y, z, sx, sy, sz, r, g, b);
        blk.textureId = assets.white;
        solids.add(blk);
        return blk;
    }

    public Block prop(Mesh m, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        Block blk = new Block(m, x, y, z, sx, sy, sz, r, g, b);
        blk.textureId = assets.white;
        props.add(blk);
        return blk;
    }

    public void loadLevel(Level lvl, boolean story) {
        level = lvl; storyMode = story;
        solids.clear(); props.clear(); enemies.clear(); boss = null; ground = null;
        for (Projectile p : pool) p.active = false;
        numbers.clear();
        if (!lvl.voidLevel) {
            ground = new Block(assets.cube, 0, -1, 0, 400, 1, 400, lvl.groundTint[0], lvl.groundTint[1], lvl.groundTint[2]);
            ground.textureId = assets.white;
            ground.tex(lvl.groundTex, lvl.groundScale).noShadow().spec(.15f);
        }
        float top = 80f, bottom = lvl.voidLevel ? -80f : 0f;
        arena.setCenterHalfExtents(0, (top + bottom) / 2f, 0, lvl.half, (top - bottom) / 2f, lvl.half);
        lvl.builder.accept(this);
        wave = 0; waveActive = false; waveTimer = 2f; levelComplete = false; completeTimer = 0f; deathTimer = 0f;
        flashT = 0f;
    }

    public void startRun(HeroDef def) {
        player = new Hero(def, assets.meshes);
        player.textureId = assets.white;
        player.transform().setPosition(0, 0, 8f);
        showcase = false;
        enemies.clear(); boss = null;
        for (Projectile p : pool) p.active = false;
        score = 0; kills = 0; wave = 0; combo = 0; comboTimer = 0f; elapsed = 0f; deathTimer = 0f;
        bannerText = ""; bannerTimer = 0f; levelComplete = false; completeTimer = 0f;
        waveActive = false; waveTimer = 2.5f; damageFlash = 0f; healFlash = 0f;
        if (level != null && level.voidLevel) player.transform().setPosition(0, .01f, 12f);
        banner(level.id > 0 ? "CHAPTER " + level.id + ": " + level.title : "SURVIVE!");
    }

    public void showcaseHero(HeroDef def) {
        player = new Hero(def, assets.meshes);
        player.textureId = assets.white;
        player.transform().setPosition(0, 0, 0);
        showcase = true;
        enemies.clear(); boss = null;
    }

    public void stop() {
        player = null; showcase = false; enemies.clear(); boss = null;
        for (Projectile p : pool) p.active = false;
    }

    // ---- small API used by heroes, enemies and the app ---------------------
    public void sfx(Sound s, Hero h) { audio.play(s, h.transform().position()); }
    public void addShake(float v) { pendingShake = Math.max(pendingShake, v); }
    public void hitStop(float s) { pendingHitStop = Math.max(pendingHitStop, s); }
    public void banner(String t) { bannerText = t; bannerTimer = 2.6f; }
    public int totalWaves() { return level.endless ? 0 : level.waves.length; }

    private void addFlash(float x, float y, float z, float r, float g, float b) {
        flashX = x; flashY = y; flashZ = z; flashR = r; flashG = g; flashB = b; flashT = 1f;
    }

    private Projectile free() {
        for (Projectile p : pool) if (!p.active) return p;
        return null;
    }

    // ---- simulation (fixed 60 Hz) ----------------------------------------
    public void updateShowcase(float dt) {
        if (player == null) return;
        player.grounded = true;
        player.moveInput.set(0, 0, 0);
        player.update(dt);
        player.animate(dt);
    }

    public void update(float dt) {
        if (player == null || showcase) return;
        elapsed += dt;
        bannerTimer = Math.max(0f, bannerTimer - dt);
        comboTimer -= dt; if (comboTimer <= 0f) combo = 0;
        damageFlash = Math.max(0f, damageFlash - dt * 2.5f);
        healFlash = Math.max(0f, healFlash - dt * 2f);
        flashT = Math.max(0f, flashT - dt * 3.5f);
        numbers.update(dt);
        if (levelComplete) completeTimer += dt;
        float groundY = level.voidLevel ? -1000f : 0f;
        Vector3f pp = player.transform().position();

        player.update(dt);
        Physics3D.integrate(player, dt, groundY);
        collide(player);
        Physics3D.clampToBounds(player, arena);
        player.animate(dt);
        playerEffects(pp);
        if (player.dead) deathTimer += dt;

        if (level.voidLevel && pp.y < -14f && !player.dead) {                 // fell off the platform
            player.transform().setPosition(0, .5f, 8f);
            player.velocity.set(0, 0, 0);
            if (player.damage(18f)) { damageFlash = 1f; audio.play(Sound.HURT, pp); }
            player.invuln = 1.2f;
        }

        if (player.poundActive && player.grounded) {                          // ground pound landing
            player.poundActive = false;
            shockwave(pp, 8.5f, 42f * player.damageMult(), 16f);
            fx.ring(pp.x, pp.y, pp.z, 8.5f, .5f, 2.5f, .5f);
            fx.dust(pp.x, pp.y, pp.z, 7f, 18);
            audio.play(Sound.SLAM, pp); addShake(.9f); hitStop(.06f);
        }
        if (player.dashDamage > 0f && player.dashTimer > 0f) {                // shield charge plows through enemies
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (e.alive && !player.dashHits.contains(e) && e.worldBounds().intersects(player.worldBounds())) {
                    player.dashHits.add(e);
                    damageEnemy(e, player.dashDamage * player.damageMult(), pp, 14f);
                    addShake(.5f); hitStop(.05f);
                }
            }
        }

        if (!player.dead) {
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (!e.alive) continue;
                e.tick(this, dt);
                Physics3D.integrate(e, dt, groundY);
                collide(e);
                Physics3D.clampToBounds(e, arena);
                if (level.voidLevel && e.transform().position().y < -14f && !e.boss) { e.health = 0f; e.alive = false; }
                float cd = e.contactDamage();
                if (e.alive && cd > 0f && e.worldBounds().intersects(player.worldBounds()))
                    damagePlayer(cd, e.transform().position(), 9f);
            }
            separateEnemies();
        }

        updateProjectiles(dt);

        boolean bossDied = false;
        for (int i = enemies.size() - 1; i >= 0; i--) {                       // process kills
            Enemy e = enemies.get(i);
            if (!e.alive) { onKill(e); if (e.boss) bossDied = true; enemies.remove(i); }
        }
        if (bossDied && !level.endless) {                                     // boss down: its minions fall with it
            for (Enemy e : enemies) {
                Vector3f p = e.transform().position();
                fx.deathBurst(p.x, p.y + 1f, p.z, 3f, 1.2f, .4f, 1f);
            }
            enemies.clear();
        }

        if (waveActive && enemies.isEmpty()) {                                // wave cleared
            waveActive = false;
            score += 50 * wave;
            player.heal(20f);
            fx.heal(pp.x, pp.y, pp.z); healFlash = 1f;
            if (!level.endless && wave >= level.waves.length) {
                levelComplete = true; completeTimer = 0f;
                banner(level.id > 0 ? "CHAPTER COMPLETE" : "VICTORY");
                audio.play(Sound.VICTORY);
            } else {
                waveTimer = 3.2f;
            }
        } else if (!waveActive && !levelComplete && !player.dead) {
            waveTimer -= dt;
            if (waveTimer <= 0f) spawnWave();
        }
    }

    private void playerEffects(Vector3f pp) {
        if (player.flying) {
            fx.thruster(pp.x - .15f, pp.y + .1f, pp.z, 0, -5f, 0);
            fx.thruster(pp.x + .15f, pp.y + .1f, pp.z, 0, -5f, 0);
        }
        if (player.rageTimer > 0f) fx.aura(pp.x, pp.y, pp.z, .4f, 2.2f, .4f);
        if (player.dashTimer > 0f) fx.trail(pp.x, pp.y + 1f, pp.z, player.def.theme[0] * 2f, player.def.theme[1] * 2f, player.def.theme[2] * 2f, .6f);
    }

    private void collide(GameObject3D o) {
        for (int i = 0; i < solids.size(); i++)
            Physics3D.resolveAgainstStatic(o, solids.get(i).worldBounds(), mtv);
    }

    private void separateEnemies() {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy a = enemies.get(i);
            if (a.boss) continue;
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy b = enemies.get(j);
                if (b.boss) continue;
                Vector3f pa = a.transform().position(), pb = b.transform().position();
                float dx = pb.x - pa.x, dz = pb.z - pa.z, d2 = dx * dx + dz * dz, min = 1.2f;
                if (d2 < min * min && d2 > 1e-6f) {
                    float d = (float) Math.sqrt(d2), push = (min - d) * .5f;
                    a.transform().translate(-dx / d * push, 0, -dz / d * push);
                    b.transform().translate(dx / d * push, 0, dz / d * push);
                }
            }
        }
    }

    // ---- waves ------------------------------------------------------------
    private WaveSpec spec(int idx) {
        if (!level.endless) return level.waves[idx];
        int n = idx + 1;
        if (n % 10 == 0) return new WaveSpec(3, 0, 0, 2);
        if (n % 5 == 0) return new WaveSpec(2, 2, 0, 1);
        return new WaveSpec(Math.min(14, 3 + n), Math.min(8, n / 2), n >= 3 ? Math.min(5, n / 3) : 0, 0);
    }

    private void spawnWave() {
        WaveSpec s = spec(wave);
        wave++;
        waveActive = true;
        for (int i = 0; i < s.troopers(); i++) spawnAtRing(Enemy.Kind.TROOPER);
        for (int i = 0; i < s.drones(); i++) spawnAtRing(Enemy.Kind.DRONE);
        for (int i = 0; i < s.brutes(); i++) spawnAtRing(Enemy.Kind.BRUTE);
        if (s.boss() > 0) {
            Enemy b = spawnEnemy(s.boss() == 1 ? Enemy.Kind.VEX : Enemy.Kind.OMEGA, level.bossX, level.bossZ);
            boss = b;
            banner("BOSS: " + b.name);
            audio.play(Sound.BOSS);
        } else {
            banner("WAVE " + wave);
            audio.play(Sound.WAVE);
        }
    }

    private void spawnAtRing(Enemy.Kind kind) {
        float x = 0, z = -20;
        Vector3f pp = player.transform().position();
        for (int tries = 0; tries < 6; tries++) {
            if (level.spawns.isEmpty()) { double a = rnd.nextDouble() * Math.PI * 2; x = (float) Math.cos(a) * 22f; z = (float) Math.sin(a) * 22f; }
            else { float[] s = level.spawns.get(rnd.nextInt(level.spawns.size())); x = s[0] + rnd.nextFloat() * 2f - 1f; z = s[1] + rnd.nextFloat() * 2f - 1f; }
            float dx = x - pp.x, dz = z - pp.z;
            if (dx * dx + dz * dz > 14f * 14f) break;
        }
        spawnEnemy(kind, x, z);
    }

    public Enemy spawnEnemy(Enemy.Kind kind, float x, float z) {
        Enemy e = new Enemy(kind, assets.meshes, wave, assets.white);
        float lim = Math.min(level.half - 1.5f, 40f);
        e.transform().setPosition(Math.max(-lim, Math.min(lim, x)), kind == Enemy.Kind.DRONE ? 3f : .05f, Math.max(-lim, Math.min(lim, z)));
        enemies.add(e);
        fx.sparks(x, 1f, z, 1.2f, .5f, 3f, 8, 5f);
        return e;
    }

    private void onKill(Enemy e) {
        Vector3f p = e.transform().position();
        score += (int) (e.score * (1f + Math.min(combo, 20) * .1f));
        combo++; comboTimer = 3.2f; kills++;
        float k = e.boss ? 3f : (e.kind == Enemy.Kind.BRUTE ? 1.6f : 1f);
        fx.deathBurst(p.x, p.y + e.barHeight * .4f, p.z, e.kind == Enemy.Kind.DRONE ? 2f : 3f, e.kind == Enemy.Kind.DRONE ? .6f : 1.2f, e.kind == Enemy.Kind.DRONE ? 3f : .3f, k);
        audio.play(Sound.KILL, p);
        if (e.boss) {
            fx.explosion(p.x, p.y + e.barHeight * .4f, p.z, e.kind == Enemy.Kind.OMEGA ? 6f : 3f);
            audio.play(Sound.EXPLOSION, p);
            hitStop(.6f); addShake(1.5f);
            banner(e.name + " DEFEATED");
            if (boss == e) boss = null;
        }
    }

    // ---- damage helpers ---------------------------------------------------
    public void damageEnemy(Enemy e, float dmg, Vector3f from, float knock) {
        boolean crit = rnd.nextFloat() < .12f;
        if (crit) dmg *= 1.6f;
        e.hurt(dmg, from, knock);
        Vector3f p = e.transform().position();
        numbers.add(p, e.barHeight, Math.round(dmg), crit);
        fx.hit(p.x, p.y + e.barHeight * .55f, p.z, 3f, 2f, 1f);
        audio.play(Sound.HIT, p);
    }

    public boolean damagePlayer(float dmg, Vector3f from, float knock) {
        if (player == null || !player.damage(dmg)) return false;
        Vector3f p = player.transform().position();
        float dx = p.x - from.x, dz = p.z - from.z, l = (float) Math.sqrt(dx * dx + dz * dz);
        if (l > 1e-3f) { dx /= l; dz /= l; }
        player.velocity.set(dx * knock * 1.2f, 4f, dz * knock * 1.2f);
        audio.play(Sound.HURT, p);
        fx.hit(p.x, p.y + 1.2f, p.z, 3f, .5f, .4f);
        addShake(.45f); damageFlash = 1f;
        return true;
    }

    public void enemySlam(float cx, float cz, float radius, float dmg, float knock, boolean needGround) {
        Vector3f p = player.transform().position();
        float dx = p.x - cx, dz = p.z - cz, r = radius + .4f;
        if (dx * dx + dz * dz <= r * r && (!needGround || p.y < 1.4f)) damagePlayer(dmg, tmp.set(cx, 0, cz), knock);
    }

    public void shockwave(Vector3f at, float radius, float dmg, float knock) {
        probe.set(at.x, at.y + .5f, at.z, radius);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds())) damageEnemy(e, dmg, at, knock);
        }
    }

    public void explode(Vector3f at, float radius, float dmg, float knock) {
        fx.explosion(at.x, at.y, at.z, radius * .45f);
        audio.play(Sound.EXPLOSION, at);
        addFlash(at.x, at.y + 1f, at.z, 3f, 1.6f, .5f);
        addShake(.5f);
        shockwave(at, radius, dmg, knock);
    }

    public void stunAround(Vector3f c, float radius, float seconds, float dmg) {
        probe.set(c.x, c.y + 1f, c.z, radius);
        fx.ring(c.x, c.y, c.z, radius, .6f, 1.6f, 3.2f);
        addFlash(c.x, c.y + 1.5f, c.z, .6f, 1.4f, 3f);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds())) { e.stun(seconds); damageEnemy(e, dmg, c, 2f); }
        }
    }

    // ---- hero abilities ---------------------------------------------------
    /** Where the crosshair points: nearest enemy or wall along the camera ray (ignoring things behind the hero). */
    private void aimPoint(Camera3D cam, Vector3f from, float maxDist, Vector3f out) {
        Vector3f o = cam.eye(), d = cam.forward();
        float minT = Math.max(0f, o.distance(from) - 1.5f), best = maxDist;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.alive) continue;
            float t = Physics3D.raycastAabb(o, d, e.worldBounds(), best);
            if (t >= minT && t < best) best = t;
        }
        for (int i = 0; i < solids.size(); i++) {
            float t = Physics3D.raycastAabb(o, d, solids.get(i).worldBounds(), best);
            if (t >= minT && t < best) best = t;
        }
        out.set(o).fma(best, d);
    }

    private void muzzleFor(Hero h, Camera3D cam, float lateral) {
        Vector3f pp = h.transform().position();
        cam.flatForward(fwd); cam.flatRight(rgt);
        muzzle.set(pp.x + fwd.x * .7f + rgt.x * lateral, pp.y + 1.35f * h.def.look.scale, pp.z + fwd.z * .7f + rgt.z * lateral);
        aimPoint(cam, pp, 75f, aim);
        dir.set(aim).sub(muzzle);
        if (dir.lengthSquared() < .25f) dir.set(cam.forward());
        dir.normalize();
    }

    public void fireBolt(Hero h, Camera3D cam, Projectile.Style style, float speed, float dmg, float lateral) {
        muzzleFor(h, cam, lateral);
        Projectile p = free();
        if (p == null) return;
        p.fire(style, muzzle, dir, speed, dmg * h.damageMult(), false);
        fx.muzzle(muzzle.x, muzzle.y, muzzle.z, p.cr * .6f, p.cg * .6f, p.cb * .6f);
    }

    /** Hitscan beam that pierces every enemy it touches (stops at walls). */
    public void fireBeam(Hero h, Camera3D cam, float dmg, float range, float width) {
        muzzleFor(h, cam, 0f);
        float len = range;
        for (int i = 0; i < solids.size(); i++) {
            float t = Physics3D.raycastAabb(muzzle, dir, solids.get(i).worldBounds(), len);
            if (t >= 0f && t < len) len = t;
        }
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.alive) continue;
            AABB b = e.worldBounds();
            float hw = width * .5f;
            beamBox.min.set(b.min.x - hw, b.min.y - hw, b.min.z - hw);
            beamBox.max.set(b.max.x + hw, b.max.y + hw, b.max.z + hw);
            float t = Physics3D.raycastAabb(muzzle, dir, beamBox, len);
            if (t >= 0f && t <= len) damageEnemy(e, dmg * h.damageMult(), muzzle, 6f);
        }
        tmp.set(muzzle).fma(len, dir);
        fx.line(muzzle.x, muzzle.y, muzzle.z, tmp.x, tmp.y, tmp.z, .45f, .5f, 2.2f, 3f, .65f, .3f);
        fx.line(muzzle.x, muzzle.y, muzzle.z, tmp.x, tmp.y, tmp.z, .9f, 2.5f, 2.8f, 3f, .3f, .22f);
        fx.explosion(tmp.x, tmp.y, tmp.z, .8f);
        addFlash(muzzle.x, muzzle.y, muzzle.z, .5f, 1.5f, 2.4f);
    }

    /** Spherical melee swing in front of the hero. @return number of enemies hit. */
    public int meleeArc(Hero h, float forward, float radius, float dmg, float knock) {
        Vector3f pp = h.transform().position();
        float fx_ = (float) Math.sin(h.aimYaw), fz_ = (float) Math.cos(h.aimYaw);
        float cx = pp.x + fx_ * forward, cy = pp.y + 1.2f * h.def.look.scale, cz = pp.z + fz_ * forward;
        probe.set(cx, cy, cz, radius);
        int n = 0;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds())) { damageEnemy(e, dmg * h.damageMult(), pp, knock); n++; }
        }
        fx.dust(cx, pp.y, cz, radius * .8f, 6);
        fx.sparks(cx, cy, cz, h.def.theme[0] * 3f, h.def.theme[1] * 3f, h.def.theme[2] * 3f, 10, 8f);
        if (h.def.id.equals("thor") && n > 0) fx.lightning(cx, cy + 6f, cz, cx, cy, cz);
        return n;
    }

    /** Call down lightning on the nearest enemies (or random ground spots if few enemies). */
    public void lightningStorm(Hero h, int strikes, float range, float dmg) {
        Vector3f pp = h.transform().position();
        boolean[] used = new boolean[enemies.size()];
        for (int s = 0; s < strikes; s++) {
            int best = -1; float bd = range * range;
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (used[i] || !e.alive) continue;
                Vector3f ep = e.transform().position();
                float dx = ep.x - pp.x, dz = ep.z - pp.z, d2 = dx * dx + dz * dz;
                if (d2 < bd) { bd = d2; best = i; }
            }
            if (best >= 0) {
                used[best] = true;
                Enemy e = enemies.get(best);
                Vector3f ep = e.transform().position();
                fx.lightning(ep.x + (rnd.nextFloat() - .5f) * 3f, ep.y + 34f, ep.z + (rnd.nextFloat() - .5f) * 3f, ep.x, ep.y + .2f, ep.z);
                e.stun(.9f);
                damageEnemy(e, dmg * h.damageMult(), ep, 8f);
                addFlash(ep.x, ep.y + 3f, ep.z, 1.2f, 1.8f, 3.2f);
            } else {
                float a = rnd.nextFloat() * 6.283f, r = 4f + rnd.nextFloat() * 14f;
                float gx = pp.x + (float) Math.cos(a) * r, gz = pp.z + (float) Math.sin(a) * r;
                fx.lightning(gx, 34f, gz, gx, .2f, gz);
            }
        }
        fx.ring(pp.x, pp.y, pp.z, 5f, 1.2f, 1.8f, 3.2f);
    }

    // ---- projectiles ------------------------------------------------------
    public void fireEnemyShot(Vector3f from, Vector3f to, float speed, float dmg, Projectile.Style style) {
        Projectile p = free();
        if (p == null) return;
        dir.set(to).sub(from).normalize();
        p.fire(style, from, dir, speed, dmg, true);
    }

    private void updateProjectiles(float dt) {
        for (Projectile p : pool) {
            if (!p.active) continue;
            p.update(dt);
            Vector3f pos = p.transform().position();
            float cx = pos.x, cy = pos.y + .3f, cz = pos.z;
            if (p.style != Projectile.Style.SHIELD) fx.trail(cx, cy, cz, p.cr, p.cg, p.cb, p.style == Projectile.Style.MISSILE ? .5f : .3f);
            AABB b = p.worldBounds();

            if (p.hostile) {
                if (!player.dead && b.intersects(player.worldBounds())) {
                    damagePlayer(p.damage, tmp.set(cx, 0, cz), 6f);
                    fx.hit(cx, cy, cz, 3f, .5f, .3f);
                    p.active = false;
                    continue;
                }
            } else {
                for (int i = 0; i < enemies.size() && p.active; i++) {
                    Enemy e = enemies.get(i);
                    if (!e.alive || e.id == p.lastHitId || !b.intersects(e.worldBounds())) continue;
                    onProjectileHit(p, e, cx, cy, cz);
                }
                if (!p.active) continue;
            }
            for (int i = 0; i < solids.size(); i++) {
                if (b.intersects(solids.get(i).worldBounds())) {
                    if (p.style == Projectile.Style.EXPLOSIVE) explode(tmp.set(cx, cy, cz), p.explodeRadius, p.damage, 12f);
                    else fx.hit(cx, cy, cz, p.cr, p.cg, p.cb);
                    p.active = false;
                    break;
                }
            }
            if (p.active && !level.voidLevel && pos.y < .05f) {
                if (p.style == Projectile.Style.EXPLOSIVE) explode(tmp.set(cx, .2f, cz), p.explodeRadius, p.damage, 12f);
                else fx.hit(cx, .2f, cz, p.cr, p.cg, p.cb);
                p.active = false;
            }
        }
    }

    private void onProjectileHit(Projectile p, Enemy e, float cx, float cy, float cz) {
        switch (p.style) {
            case EXPLOSIVE -> { explode(tmp.set(cx, cy, cz), p.explodeRadius, p.damage, 12f); p.active = false; }
            case ARROW -> {
                damageEnemy(e, p.damage, tmp.set(cx, cy, cz), 5f);
                if (p.pierce > 0) { p.pierce--; p.lastHitId = e.id; } else p.active = false;
            }
            case SHIELD -> {
                damageEnemy(e, p.damage, tmp.set(cx, cy, cz), 6f);
                p.lastHitId = e.id;
                Enemy next = null; float bd = 16f * 16f;
                for (int i = 0; i < enemies.size(); i++) {
                    Enemy o = enemies.get(i);
                    if (!o.alive || o == e) continue;
                    Vector3f op = o.transform().position();
                    float dx = op.x - cx, dz = op.z - cz, d2 = dx * dx + dz * dz;
                    if (d2 < bd) { bd = d2; next = o; }
                }
                if (p.bounces > 0 && next != null) {
                    p.bounces--;
                    Vector3f np = next.transform().position();
                    dir.set(np.x - cx, np.y + 1f - cy, np.z - cz).normalize();
                    p.velocity.set(dir).mul(38f);
                    p.setDirection(dir);
                    p.life = 3f;
                    fx.sparks(cx, cy, cz, .8f, 1.4f, 3f, 8, 6f);
                } else p.active = false;
            }
            default -> { damageEnemy(e, p.damage, tmp.set(cx, cy, cz), 5f); p.active = false; }
        }
    }

    // ---- rendering support ------------------------------------------------
    public List<GameObject3D> drawables() {
        drawList.clear();
        if (ground != null) drawList.add(ground);
        drawList.addAll(solids);
        drawList.addAll(props);
        if (player != null) drawList.add(player);
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).alive) drawList.add(enemies.get(i));
        for (Projectile p : pool) if (p.active) drawList.add(p);
        return drawList;
    }

    public Lights fillLights() {
        lights.clear();
        if (flashT > 0f) lights.add(flashX, flashY, flashZ, 14f + 10f * flashT, flashR * flashT, flashG * flashT, flashB * flashT);
        if (boss != null && boss.kind == Enemy.Kind.OMEGA) {
            Vector3f bp = boss.transform().position();
            lights.add(bp.x, bp.y + 5f, bp.z, 28f, .9f, .2f, .8f);
        }
        for (Projectile p : pool)
            if (p.active && p.style != Projectile.Style.SHIELD) {
                Vector3f pos = p.transform().position();
                lights.add(pos.x, pos.y + .3f, pos.z, 9f, p.cr * .3f, p.cg * .3f, p.cb * .3f);
            }
        return lights;
    }
}
