package com.avengers3d.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

/**
 * Pooled CPU-simulated particles drawn as camera-facing quads in two instanced draw calls
 * (additive glow, then alpha-blended smoke). Struct-of-arrays storage, no allocation while running.
 */
public final class ParticleSystem implements AutoCloseable {
    private static final int FLOATS = 8;                       // x y z size, r g b a  per instance

    private final int cap;
    private final float[] px, py, pz, vx, vy, vz, age, life, s0, s1, grav, drag;
    private final float[] c0r, c0g, c0b, c0a, c1r, c1g, c1b, c1a;
    private final boolean[] add;
    private int head;

    private final ShaderProgram shader = ShaderProgram.load("particle.vert", "particle.frag");
    private final int vao, vbo;
    private final FloatBuffer inst;

    public ParticleSystem(int capacity) {
        cap = capacity;
        px = new float[cap]; py = new float[cap]; pz = new float[cap];
        vx = new float[cap]; vy = new float[cap]; vz = new float[cap];
        age = new float[cap]; life = new float[cap]; s0 = new float[cap]; s1 = new float[cap];
        grav = new float[cap]; drag = new float[cap];
        c0r = new float[cap]; c0g = new float[cap]; c0b = new float[cap]; c0a = new float[cap];
        c1r = new float[cap]; c1g = new float[cap]; c1b = new float[cap]; c1a = new float[cap];
        add = new boolean[cap];
        for (int i = 0; i < cap; i++) { age[i] = 1f; life[i] = 0.5f; }      // all slots start dead

        inst = MemoryUtil.memAllocFloat(cap * FLOATS);
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) cap * FLOATS * 4, GL_STREAM_DRAW);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glVertexAttribDivisor(0, 1);                          // advance these attributes once per INSTANCE
        glVertexAttribDivisor(1, 1);
        glBindVertexArray(0);
    }

    /** Spawn one particle. Size and colour interpolate from the 0 values to the 1 values over its life. */
    public void spawn(float x, float y, float z, float dx, float dy, float dz, float lifeSec,
                      float size0, float size1,
                      float r0, float g0, float b0, float a0, float r1, float g1, float b1, float a1,
                      float gravity, float dragK, boolean additive) {
        int i = head;
        head = (head + 1) % cap;                              // ring buffer: the oldest particle is overwritten when full
        px[i] = x; py[i] = y; pz[i] = z; vx[i] = dx; vy[i] = dy; vz[i] = dz;
        age[i] = 0f; life[i] = Math.max(0.01f, lifeSec); s0[i] = size0; s1[i] = size1;
        c0r[i] = r0; c0g[i] = g0; c0b[i] = b0; c0a[i] = a0; c1r[i] = r1; c1g[i] = g1; c1b[i] = b1; c1a[i] = a1;
        grav[i] = gravity; drag[i] = dragK; add[i] = additive;
    }

    public void clear() { for (int i = 0; i < cap; i++) age[i] = life[i] + 1f; }

    public void update(float dt) {
        for (int i = 0; i < cap; i++) {
            if (age[i] >= life[i]) continue;
            age[i] += dt;
            float d = Math.max(0f, 1f - drag[i] * dt);
            vx[i] *= d; vy[i] = vy[i] * d - grav[i] * dt; vz[i] *= d;
            px[i] += vx[i] * dt; py[i] += vy[i] * dt; pz[i] += vz[i] * dt;
        }
    }

    private int fill(boolean additive) {
        int n = 0;
        for (int i = 0; i < cap; i++) {
            if (age[i] >= life[i] || add[i] != additive) continue;
            float t = age[i] / life[i];
            inst.put(px[i]).put(py[i]).put(pz[i]).put(s0[i] + (s1[i] - s0[i]) * t);
            inst.put(c0r[i] + (c1r[i] - c0r[i]) * t).put(c0g[i] + (c1g[i] - c0g[i]) * t)
                .put(c0b[i] + (c1b[i] - c0b[i]) * t).put(c0a[i] + (c1a[i] - c0a[i]) * t);
            n++;
        }
        return n;
    }

    public void draw(Camera3D cam) {
        inst.clear();
        int nAdd = fill(true), nAlpha = fill(false);
        if (nAdd + nAlpha == 0) return;
        inst.flip();

        shader.bind();
        shader.setMat4("uViewProj", cam.viewProjection());
        shader.setVec3("uRight", cam.right());
        shader.setVec3("uUp", cam.up());
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, inst);

        glDisable(GL_CULL_FACE);
        glDepthMask(false);                                   // glows are tested against the scene but never occlude it
        glEnable(GL_BLEND);
        if (nAdd > 0) {
            glBlendFunc(GL_ONE, GL_ONE);
            shader.setInt("uAdditive", 1);
            pointers(0);
            glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, nAdd);
        }
        if (nAlpha > 0) {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            shader.setInt("uAdditive", 0);
            pointers(nAdd);
            glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, nAlpha);
        }
        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glBindVertexArray(0);
    }

    private static void pointers(int firstInstance) {
        long base = (long) firstInstance * FLOATS * 4;
        glVertexAttribPointer(0, 4, GL_FLOAT, false, FLOATS * 4, base);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, FLOATS * 4, base + 16);
    }

    @Override public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.close();
        MemoryUtil.memFree(inst);
    }
}
