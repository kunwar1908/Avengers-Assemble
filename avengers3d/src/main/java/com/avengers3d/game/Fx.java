package com.avengers3d.game;

import com.avengers3d.render.Camera3D;
import com.avengers3d.render.Environment;
import com.avengers3d.render.ParticleSystem;

import java.util.Random;

/** Effect recipes built from particles: sparks, explosions, lightning, beams, dust, auras, ambient weather. */
public final class Fx {
    private final ParticleSystem p;
    private final Random rnd = new Random(42);
    private float ambientAcc;

    public Fx(ParticleSystem p) { this.p = p; }

    private float r(float a, float b) { return a + rnd.nextFloat() * (b - a); }

    /** Glowing additive particle that fades to transparent. HDR colour values above 1 will bloom. */
    private void glow(float x, float y, float z, float vx, float vy, float vz, float life, float s0, float s1,
                      float cr, float cg, float cb, float gravity, float drag) {
        p.spawn(x, y, z, vx, vy, vz, life, s0, s1, cr, cg, cb, 1f, cr, cg, cb, 0f, gravity, drag, true);
    }

    private void smoke(float x, float y, float z, float vx, float vy, float vz, float life, float s0, float s1,
                       float cr, float cg, float cb, float a) {
        p.spawn(x, y, z, vx, vy, vz, life, s0, s1, cr, cg, cb, a, cr, cg, cb, 0f, -0.4f, 1.5f, false);
    }

    public void sparks(float x, float y, float z, float cr, float cg, float cb, int n, float speed) {
        for (int i = 0; i < n; i++) {
            float a = r(0, 6.283f), e = r(-0.3f, 1f), s = r(0.4f, 1f) * speed;
            glow(x, y, z, (float) Math.cos(a) * s, e * s, (float) Math.sin(a) * s, r(0.25f, 0.55f), 0.13f, 0.02f, cr, cg, cb, 9f, 1.5f);
        }
    }

    public void hit(float x, float y, float z, float cr, float cg, float cb) {
        sparks(x, y, z, cr, cg, cb, 9, 7f);
        glow(x, y, z, 0, 0, 0, 0.12f, 0.5f, 1.3f, 3f, 3f, 3f, 0, 0);
    }

    public void muzzle(float x, float y, float z, float cr, float cg, float cb) {
        glow(x, y, z, 0, 0, 0, 0.08f, 0.45f, 0.9f, cr * 2f, cg * 2f, cb * 2f, 0, 0);
        sparks(x, y, z, cr, cg, cb, 4, 5f);
    }

    public void trail(float x, float y, float z, float cr, float cg, float cb, float size) {
        glow(x, y, z, r(-.3f, .3f), r(-.3f, .3f), r(-.3f, .3f), 0.25f, size, 0.02f, cr, cg, cb, 0, 2f);
    }

    public void explosion(float x, float y, float z, float radius) {
        glow(x, y, z, 0, 0, 0, 0.25f, radius * 1.3f, radius * 2.4f, 3.2f, 1.7f, 0.6f, 0, 0);
        for (int i = 0; i < 26; i++) {
            float a = r(0, 6.283f), e = r(-0.2f, 1f), s = radius * r(1.5f, 4f);
            glow(x, y, z, (float) Math.cos(a) * s, e * s, (float) Math.sin(a) * s, r(0.35f, 0.7f), radius * 0.45f, radius * 0.1f,
                 3f, r(0.8f, 1.6f), 0.3f, 2f, 3f);
        }
        for (int i = 0; i < 14; i++)
            smoke(x + r(-1, 1) * radius * 0.4f, y, z + r(-1, 1) * radius * 0.4f, r(-1, 1), r(1, 3), r(-1, 1), r(0.9f, 1.5f),
                  radius * 0.6f, radius * 1.6f, 0.18f, 0.17f, 0.17f, 0.55f);
        sparks(x, y, z, 3f, 1.6f, 0.4f, 18, radius * 4f);
    }

    public void dust(float x, float y, float z, float radius, int n) {
        for (int i = 0; i < n; i++) {
            float a = r(0, 6.283f), s = r(0.4f, 1f) * radius * 1.4f;
            smoke(x + (float) Math.cos(a) * radius * 0.3f, y + 0.15f, z + (float) Math.sin(a) * radius * 0.3f,
                  (float) Math.cos(a) * s, r(0.2f, 1.2f), (float) Math.sin(a) * s, r(0.5f, 0.9f), 0.5f, 1.3f, 0.55f, 0.5f, 0.42f, 0.5f);
        }
    }

    /** Expanding ring of light on the ground (shockwaves, telegraphs). */
    public void ring(float x, float y, float z, float radius, float cr, float cg, float cb) {
        for (int i = 0; i < 44; i++) {
            float a = i / 44f * 6.283f;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            glow(x + c * 0.5f, y + 0.2f, z + s * 0.5f, c * radius / 0.45f, 0.5f, s * radius / 0.45f, 0.45f, 0.4f, 0.12f, cr, cg, cb, 0, 0.6f);
        }
    }

    /** Call every frame while an attack is telegraphed: a thin glowing circle marks the danger zone. */
    public void groundWarning(float x, float z, float radius, float cr, float cg, float cb) {
        for (int i = 0; i < 14; i++) {
            float a = r(0, 6.283f);
            glow(x + (float) Math.cos(a) * radius, 0.15f, z + (float) Math.sin(a) * radius, 0, 0.3f, 0, 0.14f, 0.28f, 0.1f, cr, cg, cb, 0, 0);
        }
    }

    /** Dotted glowing line (laser sights, beams). */
    public void line(float x1, float y1, float z1, float x2, float y2, float z2, float spacing,
                     float cr, float cg, float cb, float size, float life) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = Math.max(1, (int) (len / spacing));
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            glow(x1 + dx * t, y1 + dy * t, z1 + dz * t, 0, 0, 0, life, size, size * 0.3f, cr, cg, cb, 0, 0);
        }
    }

    /** Jagged lightning bolt: midpoints are displaced sideways, then dotted with bright glow particles. */
    public void lightning(float x1, float y1, float z1, float x2, float y2, float z2) {
        int segs = 9;
        float px = x1, py = y1, pz = z1;
        for (int i = 1; i <= segs; i++) {
            float t = (float) i / segs;
            float amp = (i == segs) ? 0f : 1.1f * (float) Math.sin(Math.PI * t) + 0.2f;
            float nx = x1 + (x2 - x1) * t + r(-amp, amp), ny = y1 + (y2 - y1) * t, nz = z1 + (z2 - z1) * t + r(-amp, amp);
            line(px, py, pz, nx, ny, nz, 0.3f, 1.2f, 1.8f, 3.2f, 0.4f, 0.2f);
            px = nx; py = ny; pz = nz;
        }
        glow(x2, y2, z2, 0, 0, 0, 0.25f, 1.2f, 2.6f, 1.5f, 2f, 3.5f, 0, 0);
        sparks(x2, y2 + 0.2f, z2, 1.5f, 2f, 3.5f, 12, 8f);
    }

    public void thruster(float x, float y, float z, float dx, float dy, float dz) {
        glow(x, y, z, dx + r(-.4f, .4f), dy + r(-.4f, .4f), dz + r(-.4f, .4f), 0.3f, 0.28f, 0.05f, 2.6f, 1.6f, 0.6f, 0, 1f);
    }

    public void aura(float x, float y, float z, float cr, float cg, float cb) {
        glow(x + r(-.5f, .5f), y + r(0f, 2.2f), z + r(-.5f, .5f), 0, r(1f, 2.5f), 0, 0.6f, 0.2f, 0.05f, cr, cg, cb, 0, 0.5f);
    }

    public void deathBurst(float x, float y, float z, float cr, float cg, float cb, float scale) {
        sparks(x, y, z, cr, cg, cb, (int) (16 * scale), 7f * scale);
        glow(x, y, z, 0, 0, 0, 0.2f, 0.8f * scale, 2.2f * scale, cr * 1.5f, cg * 1.5f, cb * 1.5f, 0, 0);
        for (int i = 0; i < 5; i++)
            smoke(x + r(-.4f, .4f), y + r(0, .6f), z + r(-.4f, .4f), r(-1, 1), r(1, 2.5f), r(-1, 1), r(0.6f, 1.1f), 0.4f * scale, 1.2f * scale, 0.2f, 0.2f, 0.2f, 0.5f);
    }

    public void heal(float x, float y, float z) {
        for (int i = 0; i < 12; i++)
            glow(x + r(-.5f, .5f), y + r(0, 1.5f), z + r(-.5f, .5f), 0, r(1.2f, 2.6f), 0, 0.9f, 0.18f, 0.05f, 0.4f, 2.4f, 0.8f, 0, 0.3f);
    }

    /** Weather / atmosphere around the camera, chosen by the level's Environment. */
    public void ambient(Environment.Ambient type, Camera3D cam, float dt) {
        if (type == Environment.Ambient.NONE) return;
        float rate = switch (type) { case SNOW -> 90f; case EMBERS -> 28f; case DUST -> 14f; default -> 20f; };
        ambientAcc += rate * dt;
        float cx = cam.eye().x, cy = cam.eye().y, cz = cam.eye().z;
        while (ambientAcc >= 1f) {
            ambientAcc -= 1f;
            float x = cx + r(-26, 26), z = cz + r(-26, 26);
            switch (type) {
                case SNOW -> p.spawn(x, cy + r(6, 18), z, r(-1.2f, 0.2f), r(-3.2f, -1.8f), r(-.6f, .6f), 6f, 0.07f, 0.07f,
                                     1f, 1f, 1f, 0.9f, 1f, 1f, 1f, 0.9f, 0f, 0f, false);
                case EMBERS -> p.spawn(x, r(0.2f, 5f), z, r(-.5f, .5f), r(1.2f, 3.2f), r(-.5f, .5f), 3.5f, 0.1f, 0.02f,
                                       3f, 1.1f, 0.2f, 1f, 3f, 0.5f, 0.1f, 0f, 0f, 0.3f, true);
                case DUST -> p.spawn(x, r(0.5f, 8f), z, r(-.3f, .3f), r(-.1f, .2f), r(-.3f, .3f), 5f, 0.05f, 0.05f,
                                     1.2f, 1.0f, 0.8f, 0.5f, 1.2f, 1f, 0.8f, 0f, 0f, 0f, true);
                default -> p.spawn(x, r(0.2f, 6f), z, r(-.2f, .2f), r(.6f, 1.8f), r(-.2f, .2f), 4f, 0.1f, 0.03f,
                                   0.9f, 0.5f, 3f, 1f, 0.3f, 1.2f, 3f, 0f, 0f, 0.2f, true);
            }
        }
    }
}
