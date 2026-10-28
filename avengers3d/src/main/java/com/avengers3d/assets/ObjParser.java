package com.avengers3d.assets;

import com.avengers3d.render.MeshData;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wavefront OBJ -> MeshData. Runs on a worker thread; touches no OpenGL.
 * Handles v / vt / vn and faces "v", "v/vt", "v//vn", "v/vt/vn" (positive indices, n-gons fan-triangulated).
 * OBJ indexes position/uv/normal separately, but OpenGL needs ONE index per vertex, so unique
 * (v,vt,vn) triples are de-duplicated into a single interleaved-by-index vertex list.
 */
public final class ObjParser {
    private ObjParser() { }

    private static final class Floats {
        float[] a = new float[1024]; int n;
        void add(float v) { if (n == a.length) a = java.util.Arrays.copyOf(a, n * 2); a[n++] = v; }
        float[] array() { return java.util.Arrays.copyOf(a, n); }
    }

    public static MeshData parse(String path) throws IOException {
        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nor = new ArrayList<>();
        Floats outP = new Floats(), outT = new Floats(), outN = new Floats();
        List<Integer> idx = new ArrayList<>();
        Map<String, Integer> unique = new HashMap<>();

        try (BufferedReader r = Files.newBufferedReader(Path.of(path))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] t = line.trim().split("\\s+");
                if (t.length == 0) continue;
                switch (t[0]) {
                    case "v"  -> pos.add(new float[]{f(t[1]), f(t[2]), f(t[3])});
                    case "vt" -> uv.add(new float[]{f(t[1]), 1f - f(t[2])});   // flip V: OBJ origin bottom-left, image origin top-left
                    case "vn" -> nor.add(new float[]{f(t[1]), f(t[2]), f(t[3])});
                    case "f" -> {
                        int[] face = new int[t.length - 1];
                        for (int i = 1; i < t.length; i++) {
                            final String key = t[i];
                            face[i - 1] = unique.computeIfAbsent(key, k -> {
                                String[] p = k.split("/", -1);
                                float[] vp = pos.get(Integer.parseInt(p[0]) - 1);
                                outP.add(vp[0]); outP.add(vp[1]); outP.add(vp[2]);
                                if (p.length > 1 && !p[1].isEmpty()) { float[] vt = uv.get(Integer.parseInt(p[1]) - 1); outT.add(vt[0]); outT.add(vt[1]); }
                                else { outT.add(0); outT.add(0); }
                                if (p.length > 2 && !p[2].isEmpty()) { float[] vn = nor.get(Integer.parseInt(p[2]) - 1); outN.add(vn[0]); outN.add(vn[1]); outN.add(vn[2]); }
                                else { outN.add(0); outN.add(1); outN.add(0); }
                                return outP.n / 3 - 1;
                            });
                        }
                        for (int i = 1; i < face.length - 1; i++) {   // triangle fan: (0, i, i+1)
                            idx.add(face[0]); idx.add(face[i]); idx.add(face[i + 1]);
                        }
                    }
                    default -> { }
                }
            }
        }
        return new MeshData(outP.array(), outT.array(), outN.array(), idx.stream().mapToInt(Integer::intValue).toArray());
    }

    private static float f(String s) { return Float.parseFloat(s); }
}
