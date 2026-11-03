package com.avengers3d.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/** Tileable procedural textures (value-noise fBm), so the game needs no image files. Alpha = emissive mask. */
public final class ProcTextures {
    private ProcTextures() { }

    private interface Fn { void px(int x, int y, float u, float v, float[] out); }

    private static int hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
    private static float rnd(int x, int y, int seed) { return (hash(x, y, seed) & 0xFFFF) / 65535f; }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Value noise whose lattice wraps every {@code period} cells, so the texture tiles without seams. */
    private static float vnoise(float x, float y, int period, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        int x0 = Math.floorMod(xi, period), x1 = Math.floorMod(xi + 1, period);
        int y0 = Math.floorMod(yi, period), y1 = Math.floorMod(yi + 1, period);
        return lerp(lerp(rnd(x0, y0, seed), rnd(x1, y0, seed), fx), lerp(rnd(x0, y1, seed), rnd(x1, y1, seed), fx), fy);
    }
    private static float fbm(float u, float v, int base, int octaves, int seed) {
        float sum = 0, amp = 0.5f; int p = base;
        for (int o = 0; o < octaves; o++) { sum += amp * vnoise(u * p, v * p, p, seed + o); amp *= 0.5f; p *= 2; }
        return sum;
    }
    private static byte q(float v) { return (byte) Math.round(Math.max(0f, Math.min(1f, v)) * 255f); }

    private static int make(int n, Fn f) {
        ByteBuffer b = MemoryUtil.memAlloc(n * n * 4);
        float[] o = new float[4];
        try {
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++) {
                    f.px(x, y, (x + 0.5f) / n, (y + 0.5f) / n, o);
                    b.put(q(o[0])).put(q(o[1])).put(q(o[2])).put(q(o[3]));
                }
            b.flip();
            return Textures.fromBytes(n, n, b);
        } finally { MemoryUtil.memFree(b); }
    }

    public static int asphalt(int n) {
        return make(n, (x, y, u, v, o) -> {
            float g = 0.15f + 0.13f * fbm(u, v, 8, 4, 1) + (rnd(x, y, 3) - 0.5f) * 0.05f;
            if (rnd(x, y, 9) > 0.985f) g += 0.16f;
            o[0] = g; o[1] = g * 1.02f; o[2] = g * 1.08f; o[3] = 0;
        });
    }

    public static int concrete(int n) {
        return make(n, (x, y, u, v, o) -> {
            float g = 0.48f + 0.14f * fbm(u, v, 6, 4, 4) + (rnd(x, y, 5) - 0.5f) * 0.04f;
            float e = Math.min(Math.min(u * 2 % 1f, 1 - u * 2 % 1f), Math.min(v * 2 % 1f, 1 - v * 2 % 1f));
            if (e < 0.012f) g *= 0.6f;
            o[0] = g; o[1] = g; o[2] = g * 1.03f; o[3] = 0;
        });
    }

    /** Dark facade with a grid of windows; lit windows write alpha = 1 so they glow at night. */
    public static int windows(int n) {
        final int cells = 8;
        return make(n, (x, y, u, v, o) -> {
            float cu = u * cells, cv = v * cells;
            int ci = (int) cu, cj = (int) cv;
            float fu = cu - ci, fv = cv - cj;
            float body = 0.10f + 0.05f * fbm(u, v, 16, 3, 2);
            o[0] = body * 0.9f; o[1] = body; o[2] = body * 1.4f; o[3] = 0;
            if (fu > 0.18f && fu < 0.82f && fv > 0.2f && fv < 0.8f) {
                if (rnd(ci, cj, 77) < 0.40f) {
                    float warm = 0.8f + 0.2f * rnd(ci, cj, 5);
                    o[0] = 1.0f; o[1] = 0.78f * warm; o[2] = 0.40f * warm; o[3] = 1f;
                } else {
                    o[0] = 0.05f; o[1] = 0.09f; o[2] = 0.15f + 0.1f * fv; o[3] = 0;
                }
            }
        });
    }

    public static int plate(int n) {
        return make(n, (x, y, u, v, o) -> {
            float cu = u * 4 % 1f, cv = v * 4 % 1f;
            float e = Math.min(Math.min(cu, 1 - cu), Math.min(cv, 1 - cv));
            float g = 0.30f + 0.07f * fbm(u, v, 8, 3, 6) + (rnd(x, y, 7) - 0.5f) * 0.03f;
            if (e < 0.025f) g *= 0.45f;
            float rv = (float) Math.hypot(Math.min(cu, 1 - cu) - 0.06f, Math.min(cv, 1 - cv) - 0.06f);
            if (rv < 0.018f) g += 0.18f;                                   // rivets
            o[0] = g; o[1] = g * 1.02f; o[2] = g * 1.08f; o[3] = 0;
        });
    }

    public static int grate(int n) {
        return make(n, (x, y, u, v, o) -> {
            float cu = u * 16 % 1f, cv = v * 16 % 1f;
            boolean hole = cu > 0.2f && cu < 0.8f && cv > 0.2f && cv < 0.8f;
            float g = hole ? 0.02f : 0.20f + 0.08f * fbm(u, v, 8, 3, 8);
            o[0] = g; o[1] = g; o[2] = g * 1.1f; o[3] = 0;
        });
    }

    public static int snow(int n) {
        return make(n, (x, y, u, v, o) -> {
            float g = 0.80f + 0.14f * fbm(u, v, 8, 4, 10);
            if (rnd(x, y, 11) > 0.996f) g = 1.2f;                          // sparkles
            o[0] = g * 0.93f; o[1] = g * 0.97f; o[2] = g; o[3] = 0;
        });
    }

    public static int ice(int n) {
        return make(n, (x, y, u, v, o) -> {
            float f = fbm(u, v, 6, 4, 12);
            float k = 0.75f + 0.35f * f;
            if (Math.abs(fbm(u, v, 5, 3, 13) - 0.5f) < 0.012f) k += 0.35f;   // cracks
            o[0] = 0.52f * k; o[1] = 0.76f * k; o[2] = 0.95f * k; o[3] = 0.06f;
        });
    }

    public static int rock(int n) {
        return make(n, (x, y, u, v, o) -> {
            float g = 0.22f + 0.30f * fbm(u, v, 6, 5, 14);
            o[0] = g * 1.05f; o[1] = g; o[2] = g * 0.92f; o[3] = 0;
        });
    }

    /** Dark sci-fi floor with glowing cyan grid lines (alpha = 1 on the lines). */
    public static int tech(int n) {
        return make(n, (x, y, u, v, o) -> {
            float cu = u * 8 % 1f, cv = v * 8 % 1f;
            float e = Math.min(Math.min(cu, 1 - cu), Math.min(cv, 1 - cv));
            float base = 0.05f + 0.04f * fbm(u, v, 8, 3, 15);
            o[0] = base * 0.8f; o[1] = base; o[2] = base * 1.7f; o[3] = 0;
            if (e < 0.022f) { o[0] = 0.10f; o[1] = 0.85f; o[2] = 1.0f; o[3] = 1f; }
            else if (e < 0.05f) { o[0] += 0.02f; o[1] += 0.05f; o[2] += 0.09f; }
        });
    }
}
