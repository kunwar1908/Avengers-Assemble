package com.avengers3d.render;

/** Up to 8 dynamic point lights (muzzle flashes, explosions, lightning) uploaded to the scene shader each frame. */
public final class Lights {
    public static final int MAX = 8;
    public final float[] pos = new float[MAX * 4];   // x, y, z, radius
    public final float[] col = new float[MAX * 3];
    public int count;

    public void clear() { count = 0; }

    public void add(float x, float y, float z, float radius, float r, float g, float b) {
        if (count >= MAX) return;
        int i = count++;
        pos[i * 4] = x; pos[i * 4 + 1] = y; pos[i * 4 + 2] = z; pos[i * 4 + 3] = radius;
        col[i * 3] = r; col[i * 3 + 1] = g; col[i * 3 + 2] = b;
    }
}
