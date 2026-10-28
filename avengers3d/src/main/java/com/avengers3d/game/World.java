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
import com.avengers3d.scene.Thor;
import com.avengers3d.scene.CaptainAmerica;
import com.avengers3d.scene.BlackWidow;
import com.avengers3d.scene.GenericHero;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Game rules: arena, waves, combat, scoring. Owns every entity; GameApp owns the window/state machine. */
public final class World {
    public enum Hero { HULK, IRON_MAN, THOR, CAPTAIN_AMERICA, BLACK_WIDOW,
        HAWKEYE, WEB_RUNNER, MYSTIC_WEAVER, VISIONARY, PANTHER, SIZE_SHIFTER,
        WINTER_SOLDIER, ARCANE_SAGE, SOLAR_VANGUARD }

    public Character3D player;
    public int score, wave, kills;
    public float bannerTimer;
    public final List<Enemy> enemies = new ArrayList<>();
    private final List<CombatEffect> effects = new ArrayList<>();

    private final Mesh cube, sphere;
    private final int whiteTex;
    private final AudioManager audio;
    private final List<Block> blocks = new ArrayList<>();
    private final Projectile[] projectiles = new Projectile[48];
    private final List<GameObject3D> drawList = new ArrayList<>(200);
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
        player = switch (hero) {
            case HULK -> new Hulk(cube);
            case IRON_MAN -> new IronMan(cube);
            case THOR -> new Thor(cube);
            case CAPTAIN_AMERICA -> new CaptainAmerica(cube);
            case BLACK_WIDOW -> new BlackWidow(cube);
            case HAWKEYE -> new GenericHero(GenericHero.Kind.HAWKEYE, cube);
            case WEB_RUNNER -> new GenericHero(GenericHero.Kind.WEB_RUNNER, cube);
            case MYSTIC_WEAVER -> new GenericHero(GenericHero.Kind.MYSTIC_WEAVER, cube);
            case VISIONARY -> new GenericHero(GenericHero.Kind.VISIONARY, cube);
            case PANTHER -> new GenericHero(GenericHero.Kind.PANTHER, cube);
            case SIZE_SHIFTER -> new GenericHero(GenericHero.Kind.SIZE_SHIFTER, cube);
            case WINTER_SOLDIER -> new GenericHero(GenericHero.Kind.WINTER_SOLDIER, cube);
            case ARCANE_SAGE -> new GenericHero(GenericHero.Kind.ARCANE_SAGE, cube);
            case SOLAR_VANGUARD -> new GenericHero(GenericHero.Kind.SOLAR_VANGUARD, cube);
        };
        player.textureId = whiteTex;
        player.transform().setPosition(0, 0, 0);
        enemies.clear();
        effects.clear();
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
        for (int i = effects.size() - 1; i >= 0; i--)
            if (!effects.get(i).updateEffect(dt)) effects.remove(i);

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
                    effectAt(p.transform().position(), 0.45f, 0.2f, 0.85f, 1f);
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

    /** Normal attack: melee for brawlers and a camera-aimed projectile for ranged heroes. */
    public void heroAttack(Camera3D cam) {
        if (player == null || player.cooldown > 0f) return;
        Vector3f pp = player.transform().position();
        cam.flatForward(fwd);
        player.faceYaw(cam.yaw());
        effectAt(new Vector3f(pp.x + fwd.x * 0.9f, pp.y + 1.1f, pp.z + fwd.z * 0.9f),
                0.35f, 0.35f, 0.85f, 1f);
        if (player instanceof Hulk || player instanceof Thor) {
            player.cooldown = player instanceof Hulk ? 0.55f : 1.1f;
            probe.set(pp.x + fwd.x * 2f, pp.y + 1.5f, pp.z + fwd.z * 2f, player instanceof Hulk ? 3.4f : 5.5f);
            for (Enemy e : enemies)
                if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                    damageEnemy(e, player instanceof Hulk ? 35f : 55f, pp, 14f);
            audio.play(Sound.SMASH);
        } else if (player instanceof CaptainAmerica) {
            player.cooldown = 0.48f;
            fireShield(cam, pp, fwd);
        } else {
            player.cooldown = player instanceof BlackWidow ? 0.08f : 0.16f;
            fireAimedProjectile(cam, pp, fwd, 55f, player instanceof BlackWidow ? 8f : 12f, 0);
        }
    }

    /** Shared Q-key special powers for the expanded roster. */
    public void heroSpecial(Camera3D cam) {
                if (player == null || player.specialCooldown > 0f) return;
                Vector3f pp = player.transform().position();
                cam.flatForward(fwd);
                player.faceYaw(cam.yaw());
                effectAt(pp, 1.3f, 0.8f, 0.3f, 1f);

                if (player instanceof Hulk) {
                    player.specialCooldown = 4f;
                    probe.set(pp.x, pp.y + 0.5f, pp.z, 8f);
                    for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                        damageEnemy(e, 70f, pp, 20f);
                    audio.play(Sound.SMASH);
                } else if (player instanceof IronMan) {
                    player.specialCooldown = 3f;
                    for (int i = 0; i < 5; i++) fireAimedProjectile(cam, pp, fwd, 60f, 18f, i - 2);
                } else if (player instanceof Thor) {
                    player.specialCooldown = 4f;
                    probe.set(pp.x + fwd.x * 3f, pp.y + 1f, pp.z + fwd.z * 3f, 7f);
                    for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                        damageEnemy(e, 90f, pp, 24f);
                    audio.play(Sound.SMASH);
                } else if (player instanceof CaptainAmerica) {
                    player.specialCooldown = 3f;
                    probe.set(pp.x + fwd.x * 5f, pp.y + 1f, pp.z + fwd.z * 5f, 4f);
                    for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                        damageEnemy(e, 75f, pp, 20f);
                    audio.play(Sound.HIT);
                } else if (player instanceof BlackWidow) {
                    player.specialCooldown = 2.5f;
                    for (int i = 0; i < 8; i++) fireAimedProjectile(cam, pp, fwd, 70f, 14f, i - 4);
                } else if (player instanceof GenericHero hero) {
                    player.specialCooldown = switch (hero.kind()) {
                        case HAWKEYE, WINTER_SOLDIER -> 2.5f;
                        case WEB_RUNNER, PANTHER -> 3f;
                        case MYSTIC_WEAVER, ARCANE_SAGE -> 4f;
                        case VISIONARY -> 3.5f;
                        case SIZE_SHIFTER -> 4f;
                        case SOLAR_VANGUARD -> 5f;
                    };
                    switch (hero.kind()) {
                        case HAWKEYE, WINTER_SOLDIER -> {
                            for (int i = 0; i < 4; i++) fireAimedProjectile(cam, pp, fwd, 70f, 22f, i - 2);
                        }
                        case WEB_RUNNER, PANTHER -> {
                            probe.set(pp.x + fwd.x * 3f, pp.y + 1f, pp.z + fwd.z * 3f, 5f);
                            for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                                damageEnemy(e, 65f, pp, 22f);
                        }
                        case MYSTIC_WEAVER, ARCANE_SAGE -> {
                            probe.set(pp.x, pp.y + 1f, pp.z, 8f);
                            for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                                damageEnemy(e, 80f, pp, 16f);
                        }
                        case VISIONARY -> {
                            player.heal(30f);
                            for (int i = 0; i < 3; i++) fireAimedProjectile(cam, pp, fwd, 60f, 20f, i - 1);
                        }
                        case SIZE_SHIFTER -> {
                            player.heal(20f);
                            probe.set(pp.x, pp.y + 1f, pp.z, 4f);
                            for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                                damageEnemy(e, 100f, pp, 28f);
                        }
                        case SOLAR_VANGUARD -> {
                            player.heal(45f);
                            probe.set(pp.x, pp.y + 1f, pp.z, 9f);
                            for (Enemy e : enemies) if (e.alive && Physics3D.sphereVsAabb(probe, e.worldBounds()))
                                damageEnemy(e, 110f, pp, 30f);
                        }
                    }
                }
                audio.play(Sound.SHOOT);
            }

    private void fireAimedProjectile(Camera3D cam, Vector3f pp, Vector3f forward, float speed,
                                     float damage, int spreadIndex) {
        aim.set(cam.eye()).fma(60f, cam.forward());
        muzzle.set(pp.x + forward.x * 0.7f, pp.y + 1.2f, pp.z + forward.z * 0.7f);
        dir.set(aim).sub(muzzle).normalize();
        dir.x += spreadIndex * 0.025f;
        dir.z += spreadIndex * 0.012f;
        dir.normalize();
        fireProjectile(dir, muzzle, speed, damage);
    }

    private void fireShield(Camera3D cam, Vector3f pp, Vector3f forward) {
        Vector3f o = cam.eye(), d = cam.forward();
        aim.set(o).fma(60f, d);
        muzzle.set(pp.x + forward.x * 0.8f, pp.y + 1.2f, pp.z + forward.z * 0.8f);
        dir.set(aim).sub(muzzle).normalize();
        fireProjectile(dir, muzzle, 45f, 32f);
        audio.play(Sound.SHOOT);
    }

    private void fireProjectile(Vector3f direction, Vector3f origin, float speed, float damage) {
        for (Projectile p : projectiles)
            if (!p.active) { p.fire(origin, direction, speed, damage); break; }
        audio.play(Sound.SHOOT);
    }

    private void effectAt(Vector3f position, float radius, float r, float g, float b) {
        CombatEffect effect = new CombatEffect(sphere, whiteTex, radius, r, g, b, 0.22f);
        effect.transform().setPosition(position.x, position.y, position.z);
        effects.add(effect);
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
        drawList.addAll(effects);
        return drawList;
    }
}
