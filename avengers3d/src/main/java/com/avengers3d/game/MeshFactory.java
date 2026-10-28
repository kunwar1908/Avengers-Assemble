package com.avengers3d.game;

import com.avengers3d.render.MeshData;

/** Procedural primitives, so the game runs with zero model files. Swap in OBJ meshes via AssetManager later. */
public final class MeshFactory {
    private MeshFactory() { }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
    private static float[] norm(float[] a) {
        float l = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new float[]{a[0] / l, a[1] / l, a[2] / l};
    }

    /**
     * Unit cube (edge 1) centred on the origin. For each face normal n we pick tangents u, v with
     * u x v = n, then emit corners in (-,-) (+,-) (+,+) (-,+) order, which is counter-clockwise
     * when seen from outside, so back-face culling keeps the visible side.
     */
    public static MeshData cube() {
        float[][] normals = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        float[][] corner = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        float[][] cuv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        float[] pos = new float[24 * 3], uv = new float[24 * 2], nor = new float[24 * 3];
        int[] idx = new int[36];
        for (int f = 0; f < 6; f++) {
            float[] n = normals[f];
            float[] helper = Math.abs(n[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
            float[] u = norm(cross(helper, n));
            float[] v = cross(n, u);
            for (int c = 0; c < 4; c++) {
                int vi = f * 4 + c;
                for (int k = 0; k < 3; k++) {
                    pos[vi * 3 + k] = 0.5f * (n[k] + corner[c][0] * u[k] + corner[c][1] * v[k]);
                    nor[vi * 3 + k] = n[k];
                }
                uv[vi * 2] = cuv[c][0];
                uv[vi * 2 + 1] = cuv[c][1];
            }
            int b = f * 4, i = f * 6;
            idx[i] = b; idx[i + 1] = b + 1; idx[i + 2] = b + 2;
            idx[i + 3] = b; idx[i + 4] = b + 2; idx[i + 5] = b + 3;
        }
        return new MeshData(pos, uv, nor, idx);
    }

    /** Unit-diameter UV sphere: x = sin(t)cos(p), y = cos(t), z = sin(t)sin(p). The normal equals the position. */
    public static MeshData sphere(int stacks, int slices) {
        int vc = (stacks + 1) * (slices + 1);
        float[] pos = new float[vc * 3], uv = new float[vc * 2], nor = new float[vc * 3];
        int[] idx = new int[stacks * slices * 6];
        int vi = 0;
        for (int i = 0; i <= stacks; i++) {
            double th = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double ph = 2 * Math.PI * j / slices;
                float x = (float) (Math.sin(th) * Math.cos(ph)), y = (float) Math.cos(th), z = (float) (Math.sin(th) * Math.sin(ph));
                pos[vi * 3] = 0.5f * x; pos[vi * 3 + 1] = 0.5f * y; pos[vi * 3 + 2] = 0.5f * z;
                nor[vi * 3] = x; nor[vi * 3 + 1] = y; nor[vi * 3 + 2] = z;
                uv[vi * 2] = (float) j / slices; uv[vi * 2 + 1] = (float) i / stacks;
                vi++;
            }
        }
        int k = 0;
        for (int i = 0; i < stacks; i++)
            for (int j = 0; j < slices; j++) {
                int a = i * (slices + 1) + j, b = a + slices + 1;
                idx[k++] = a; idx[k++] = a + 1; idx[k++] = b;
                idx[k++] = a + 1; idx[k++] = b + 1; idx[k++] = b;
            }
        return new MeshData(pos, uv, nor, idx);
    }

    /** Flat square on y = 0, spanning [-half, half]; UVs tile {@code uvRepeat} times (texture wrap = REPEAT). */
    public static MeshData plane(float half, float uvRepeat) {
        float[] pos = {-half, 0, -half, -half, 0, half, half, 0, half, half, 0, -half};
        float[] nor = {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0};
        float[] uv = {0, 0, 0, uvRepeat, uvRepeat, uvRepeat, uvRepeat, 0};
        return new MeshData(pos, uv, nor, new int[]{0, 1, 2, 0, 2, 3});
    }
}
