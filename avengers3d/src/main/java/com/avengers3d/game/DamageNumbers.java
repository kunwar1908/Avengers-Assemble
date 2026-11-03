package com.avengers3d.game;

import com.avengers3d.render.Camera3D;
import com.avengers3d.render.UIRenderer;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Floating combat text: world positions are projected through the view-projection matrix to screen pixels. */
public final class DamageNumbers {
    private static final int N = 48;
    private static final String[] CACHE = new String[1000];

    private final float[] x = new float[N], y = new float[N], z = new float[N], life = new float[N];
    private final boolean[] crit = new boolean[N];
    private final String[] label = new String[N];
    private int head;
    private final Vector4f tmp = new Vector4f();

    private static String text(int v) {
        v = Math.max(0, Math.min(CACHE.length - 1, v));
        if (CACHE[v] == null) CACHE[v] = Integer.toString(v);
        return CACHE[v];
    }

    public void add(Vector3f pos, float height, int value, boolean big) {
        int i = head; head = (head + 1) % N;
        x[i] = pos.x + (float) (Math.random() - .5) * .6f; y[i] = pos.y + height; z[i] = pos.z + (float) (Math.random() - .5) * .6f;
        life[i] = 0.9f; crit[i] = big; label[i] = text(value);
    }

    public void clear() { for (int i = 0; i < N; i++) life[i] = 0; }

    public void update(float dt) {
        for (int i = 0; i < N; i++) if (life[i] > 0) { life[i] -= dt; y[i] += dt * 1.6f; }
    }

    public void draw(UIRenderer ui, Camera3D cam, float w, float h, float uiScale) {
        for (int i = 0; i < N; i++) {
            if (life[i] <= 0) continue;
            tmp.set(x[i], y[i], z[i], 1f);
            cam.viewProjection().transform(tmp);
            if (tmp.w <= 0.1f) continue;
            float sx = (tmp.x / tmp.w * .5f + .5f) * w, sy = (1f - (tmp.y / tmp.w * .5f + .5f)) * h;
            float sc = (crit[i] ? 3.4f : 2.4f) * uiScale;
            if (crit[i]) ui.text(label[i], sx - ui.textWidth(label[i], sc) / 2f, sy, sc, 1f, .85f, .2f);
            else ui.text(label[i], sx - ui.textWidth(label[i], sc) / 2f, sy, sc, 1f, 1f, 1f);
        }
    }
}
