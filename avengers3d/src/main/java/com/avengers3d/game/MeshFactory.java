package com.avengers3d.game;

import com.avengers3d.render.MeshData;

import java.util.ArrayList;
import java.util.List;

/** Procedural primitives, so the game runs with zero model files. All meshes are counter-clockwise outward. */
public final class MeshFactory {
    private MeshFactory() { }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
    private static float[] norm(float[] a) {
        float l = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new float[]{a[0] / l, a[1] / l, a[2] / l};
    }

    /** Unit cube (edge 1) centred on the origin. */
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
            float[] v = cross(n, u);                         // u x v = n  ->  corners below are CCW seen from outside
            for (int c = 0; c < 4; c++) {
                int vi = f * 4 + c;
                for (int k = 0; k < 3; k++) {
                    pos[vi * 3 + k] = 0.5f * (n[k] + corner[c][0] * u[k] + corner[c][1] * v[k]);
                    nor[vi * 3 + k] = n[k];
                }
                uv[vi * 2] = cuv[c][0]; uv[vi * 2 + 1] = cuv[c][1];
            }
            int b = f * 4, i = f * 6;
            idx[i] = b; idx[i + 1] = b + 1; idx[i + 2] = b + 2; idx[i + 3] = b; idx[i + 4] = b + 2; idx[i + 5] = b + 3;
        }
        return new MeshData(pos, uv, nor, idx);
    }

    /** Unit-diameter UV sphere. */
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

    private static final class Builder {
        final List<Float> p = new ArrayList<>(), t = new ArrayList<>(), n = new ArrayList<>();
        final List<Integer> idx = new ArrayList<>();
        int vert(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
            p.add(x); p.add(y); p.add(z); n.add(nx); n.add(ny); n.add(nz); t.add(u); t.add(v);
            return p.size() / 3 - 1;
        }
        void tri(int a, int b, int c) { idx.add(a); idx.add(b); idx.add(c); }
        MeshData build() {
            float[] pp = new float[p.size()], tt = new float[t.size()], nn = new float[n.size()];
            for (int i = 0; i < pp.length; i++) pp[i] = p.get(i);
            for (int i = 0; i < tt.length; i++) tt[i] = t.get(i);
            for (int i = 0; i < nn.length; i++) nn[i] = n.get(i);
            return new MeshData(pp, tt, nn, idx.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    /** Cylinder: radius 0.5, height 1, axis = Y, centred on the origin. */
    public static MeshData cylinder(int seg) {
        Builder b = new Builder();
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            int top0 = b.vert(.5f * c0, .5f, .5f * s0, c0, 0, s0, (float) i / seg, 1);
            int bot0 = b.vert(.5f * c0, -.5f, .5f * s0, c0, 0, s0, (float) i / seg, 0);
            int top1 = b.vert(.5f * c1, .5f, .5f * s1, c1, 0, s1, (float) (i + 1) / seg, 1);
            int bot1 = b.vert(.5f * c1, -.5f, .5f * s1, c1, 0, s1, (float) (i + 1) / seg, 0);
            b.tri(top0, top1, bot0); b.tri(top1, bot1, bot0);
            int tc = b.vert(0, .5f, 0, 0, 1, 0, .5f, .5f);
            int t0 = b.vert(.5f * c0, .5f, .5f * s0, 0, 1, 0, .5f + .5f * c0, .5f + .5f * s0);
            int t1 = b.vert(.5f * c1, .5f, .5f * s1, 0, 1, 0, .5f + .5f * c1, .5f + .5f * s1);
            b.tri(tc, t1, t0);                                    // top cap, CCW seen from above
            int bc = b.vert(0, -.5f, 0, 0, -1, 0, .5f, .5f);
            int b0 = b.vert(.5f * c0, -.5f, .5f * s0, 0, -1, 0, .5f + .5f * c0, .5f + .5f * s0);
            int b1 = b.vert(.5f * c1, -.5f, .5f * s1, 0, -1, 0, .5f + .5f * c1, .5f + .5f * s1);
            b.tri(bc, b0, b1);                                    // bottom cap, CCW seen from below
        }
        return b.build();
    }

    /** Cone: base radius 0.5 at y = -0.5, apex at y = +0.5. */
    public static MeshData cone(int seg) {
        Builder b = new Builder();
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg, am = (a0 + a1) / 2;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float nx = (float) Math.cos(am) * .894f, nz = (float) Math.sin(am) * .894f, ny = .447f;   // slope normal for h = 2r
            int base0 = b.vert(.5f * c0, -.5f, .5f * s0, nx, ny, nz, 0, 0);
            int base1 = b.vert(.5f * c1, -.5f, .5f * s1, nx, ny, nz, 1, 0);
            int apex = b.vert(0, .5f, 0, nx, ny, nz, .5f, 1);
            b.tri(base0, apex, base1);
            int bc = b.vert(0, -.5f, 0, 0, -1, 0, .5f, .5f);
            int q0 = b.vert(.5f * c0, -.5f, .5f * s0, 0, -1, 0, .5f + .5f * c0, .5f + .5f * s0);
            int q1 = b.vert(.5f * c1, -.5f, .5f * s1, 0, -1, 0, .5f + .5f * c1, .5f + .5f * s1);
            b.tri(bc, q0, q1);
        }
        return b.build();
    }
}
