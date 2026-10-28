package com.avengers3d.game;

import com.avengers3d.audio.AudioManager;
import com.avengers3d.audio.AudioManager.Sound;
import com.avengers3d.physics.AABB;
import com.avengers3d.physics.BoundingSphere;
import com.avengers3d.physics.Physics3D;
import com.avengers3d.render.Camera3D;
import com.avengers3d.render.Mesh;
import com.avengers3d.scene.Character3D;
import com.avengers3d.scene.GameObject3D;
import com.avengers3d.scene.Hulk;
import com.avengers3d.scene.IronMan;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Game rules: arena, waves, combat, scoring. Owns every entity; GameApp owns the window/state machine. */
public final class World {
    public enum Hero { HULK, IRON_MAN }

    public Character3D player;
    public int score, wave, kills;
    public float bannerTimer;
    public final List<Enemy> enemies = new ArrayList<>();

    private final Mesh cube, sphere;
    private final int whiteTex;
    private final AudioManager audio;
    private final List<Block> blocks = new ArrayList<>();
    private final Projectile[] projectiles = new Projectile[48];
    private final List<GameObject3D> drawList = new ArrayList<>(160);
    private final GameObject3D ground;
    private final AABB arena = new AABB().setCenterHalfExtents(0, 20, 0, 29.5f, 20, 29.5f);

    private boolean waveInProgress;
    private float waveTimer;
    private final Random rng = new Random();

    // scratch objects (reused every frame: no GC pressure)
    private final Vector3f mtv = new Vector3f(), fwd = new Vector3f(), aim = new Vector3f(),
                           muzzle = new Vector3f(), dir = new Vector3f();
    private final BoundingSphere probe = new BoundingSphere();

    public World(Mesh cube, Mesh sphere, Mesh plane, int whiteTex, int checkerTex, AudioManager audio) {
        this.cube = cube; this.sphere = sphere; this.whiteTex = whiteTex; this.audio = audio;

        ground = new GameObject3D() { @Override public void update(float dt) { } };
        ground.textureId = checkerTex;
        ground.addPart(plane, 0, 0, 0, 1, 1, 1, 1f, 1f, 1f);

        //            x     y    z     sx    sy    sz     colour
        block(  8,  0,   0,   6,   2,   6, 0.45f, 0.5f, 0.6f);
        block(-12,  0,   8,   5,   3,   5, 0.55f, 0.45f, 0.4f);
        block(  0,  0, -14,  10, 1.2f,  4, 0.5f, 0.5f, 0.55f);
        block( 14,  0, -14,   4,   5,   4, 0.4f, 0.45f, 0.6f);
        block(-14,  0, -14,   6, 2.5f,  6, 0.6f, 0.5f, 0.4f);
        block( -6,  5,  -2,   6, 0.6f,  6, 0.7f, 0.7f, 0.8f);          // floating platform
        block(  0,  0, -30.5f, 62, 4, 1, 0.3f, 0.3f, 0.35f);           // arena walls
        block(  0,  0,  30.5f, 62, 4, 1, 0.3f, 0.3f, 0.35f);
        block(-30.5f, 0,  0,    1, 4, 62, 0.3f, 0.3f, 0.35f);
        block( 30.5f, 0,  0,    1, 4, 62, 0.3f, 0.3f, 0.35f);

        for (int i = 0; i < projectiles.length; i++) projectiles[i] = new Projectile(sphere, whiteTex);
    }

    private void block(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        blocks.add(new Block(cube, whiteTex, x, y, z, sx, sy, sz, r, g, b));
    }

    // ---- lifecycle --------------------------------------------------------
    public void start(Hero hero) {
        player = (hero == Hero.HULK) ? new Hulk(cube) : new IronMan(cube);
        player.textureId = whiteTex;
        player.transform().setPosition(0, 0, 0);
        enemies.clear();
        for (Projectile p : projectiles) p.active = false;
        score = 0; wave = 0; kills = 0; bannerTimer = 0;
        waveInProgress = false;
        waveTimer = 1.5f;
    }

    public void stop() {
        player = null;
        enemies.clear();
        for (Projectile p : projectiles) p.active = false;
    }

    // ---- simulation (fixed 60 Hz) ----------------------------------------
    public void update(float dt) {
        if (player == null) return;
        bannerTimer = Math.max(0f, bannerTimer - dt);

        // 1) player: intent -> velocity -> integrate -> collide -> clamp
        player.update(dt);
        Physics3D.integrate(player, dt, 0f);
        collideBlocks(player);
        Physics3D.clampToBounds(player, arena);

        if (player instanceof Hulk h && h.poundActive && player.grounded) {   // landed from a ground pound
            h.poundActive = false;
            shockwave();
        }

        // 2) enemies
        Vector3f pp = player.transform().position();
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.update(dt);
            Physics3D.integrate(e, dt, 0f);
            collideBlocks(e);
            Physics3D.clampToBounds(e, arena);

            if (e.alive && e.worldBounds().intersects(player.worldBounds()) && player.damage(e.contactDamage)) {
                Vector3f ep = e.transform().position();
                float dx = pp.x - ep.x, dz = pp.z - ep.z, len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len > 1e-3f) { dx /= len; dz /= len; }
                player.velocity.set(dx * 9f, 5f, dz * 9f);                    // knockback
                audio.play(Sound.HURT);
            }
        }
        separateEnemies();

        // 3) projectiles
        for (Projectile p : projectiles) {
            if (!p.active) continue;
            p.update(dt);
            AABB pb = p.worldBounds();
            for (int i = 0; i < enemies.size() && p.active; i++) {
                Enemy e = enemies.get(i);
                if (e.alive && pb.intersects(e.worldBounds())) {
                    damageEnemy(e, p.damage, p.transform().position(), 4f);
                    p.active = false;
                }
            }
            for (int i = 0; i < blocks.size() && p.active; i++)
                if (pb.intersects(blocks.get(i).worldBounds())) p.active = false;
            if (p.transform().position().y < -0.2f) p.active = false;
        }

        // 4) remove the dead
        for (int i = enemies.size() - 1; i >= 0; i--) if (!enemies.get(i).alive) enemies.remove(i);

        // 5) waves
        if (waveInProgress && enemies.isEmpty()) {
            waveInProgress = false;
            score += 50 * wave;                  // clear bonus
            player.heal(25f);
            waveTimer = 3f;
        } else if (!waveInProgress) {
            waveTimer -= dt;
            if (waveTimer <= 0f) spawnWave();
        }
    }

    private void collideBlocks(GameObject3D o) {
        for (int i = 0; i < blocks.size(); i++)
            Physics3D.resolveAgainstStatic(o, blocks.get(i).worldBounds(), mtv);
    }

    /** Cheap crowd separation so enemies don't stack into one blob. */
    private void separateEnemies() {
        final float min = 1.1f, min2 = min * min;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy a = enemies.get(i);
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy b = enemies.get(j);
                Vector3f pa = a.transform().position(), pb = b.transform().position();
                float dx = pb.x - pa.x, dz = pb.z - pa.z, d2 = dx * dx + dz * dz;
                if (d2 < min2 && d2 > 1e-6f) {
                    float d = (float) Math.sqrt(d2), push = (min - d) * 0.5f;
                    dx /= d; dz /= d;
                    a.transform().translate(-dx * push, 0, -dz * push);
                    b.transform().translate(dx * push, 0, dz * push);
                }
            }
        }
    }

    private void spawnWave() {
        wave++;
        bannerTimer = 2f;
        waveInProgress = true;
        int count = Math.min(3 + wave * 2, 22);
        float droneChance = Math.min(0.5f, 0.15f + 0.04f * wave);
        for (int i = 0; i < count; i++) {
            Enemy.Kind k = rng.nextFloat() < droneChance ? Enemy.Kind.DRONE : Enemy.Kind.SOLDIER;
            Enemy e = new Enemy(k, cube, sphere, player.transform().position(), wave, whiteTex);
            double a = rng.nextDouble() * Math.PI * 2;
            float r = 18f + rng.nextFloat() * 9f;
            e.transform().setPosition((float) Math.cos(a) * r, k == Enemy.Kind.DRONE ? 3f : 0f, (float) Math.sin(a) * r);
            enemies.add(e);
        }
        audio.play(Sound.WAVE);
    }

    private void damageEnemy(Enemy e, float amount, Vector3f from, float knockback) {
        e.damage(amount, from, knockback);
        if (e.alive) {
            audio.play(Sound.HIT);
        } else {
            score += e.scoreValue;
            kills++;
            audio.play(Sound.KILL);
        }
    }

    // ---- hero abilities ---------------------------------------------------
    private void shockwave() {
        Vector3f pp = player.transform().position();
        probe.set(pp.x, pp.y + 0.5f, pp.z, 7f);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds())) damageEnemy(e, 45f, pp, 14f);
        }
        audio.play(Sound.SMASH);
    }

    /** Hulk: spherical smash in front of him. Iron Man: repulsor bolt aimed at the crosshair via a camera raycast. */
    public void heroAttack(Camera3D cam) {
        if (player == null || player.cooldown > 0f) return;
        Vector3f pp = player.transform().position();
        cam.flatForward(fwd);
        player.faceYaw(cam.yaw());

        if (player instanceof Hulk) {
            player.cooldown = 0.55f;
            probe.set(pp.x + fwd.x * 2f, pp.y + 1.6f, pp.z + fwd.z * 2f, 3.4f);
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds())) damageEnemy(e, 35f, pp, 12f);
            }
            audio.play(Sound.SMASH);
        } else {
            player.cooldown = 0.16f;
            // Find what the crosshair points at: nearest ray/AABB hit, else a point 60 m down the ray.
            Vector3f o = cam.eye(), d = cam.forward();
            float best = 60f;
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (!e.alive) continue;
                float t = Physics3D.raycastAabb(o, d, e.worldBounds(), 100f);
                if (t >= 0f && t < best) best = t;
            }
            aim.set(o).fma(best, d);
            muzzle.set(pp.x + fwd.x * 0.6f, pp.y + 1.4f, pp.z + fwd.z * 0.6f);
            dir.set(aim).sub(muzzle).normalize();                     // bolt flies muzzle -> aim point
            for (Projectile p : projectiles)
                if (!p.active) { p.fire(muzzle, dir, 55f, 12f); break; }
            audio.play(Sound.SHOOT);
        }
    }

    // ---- rendering support -----------------------------------------------
    /** Rebuilt each frame into a pre-sized list: no allocation once warmed up. */
    public List<GameObject3D> drawables() {
        drawList.clear();
        drawList.add(ground);
        drawList.addAll(blocks);
        if (player != null) drawList.add(player);
        drawList.addAll(enemies);
        for (Projectile p : projectiles) if (p.active) drawList.add(p);
        return drawList;
    }
}
