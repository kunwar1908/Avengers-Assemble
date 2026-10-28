package com.avengers3d.render;

import org.lwjgl.stb.STBEasyFont;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Immediate-mode 2D overlay batched into ONE draw call per frame.
 * Everything (rectangles and text) is a quad with 16-byte vertices: float x, y, z + 4 colour bytes.
 * Text comes from stb_easy_font (vector-quad font bundled with LWJGL-stb), so no font file is needed.
 * Coordinates are pixels with the origin at the top-left.
 */
public final class UIRenderer implements AutoCloseable {
    private static final int MAX_QUADS = 4096;
    private static final String VS = """
        #version 330 core
        layout(location = 0) in vec3 aPos;
        layout(location = 1) in vec4 aColor;
        uniform vec2 uScreen;
        out vec4 vColor;
        void main() {
            // pixels -> NDC: x: [0,W] -> [-1,1], y: [0,H] -> [1,-1] (flip, GL's y points up)
            gl_Position = vec4(aPos.x / uScreen.x * 2.0 - 1.0, 1.0 - aPos.y / uScreen.y * 2.0, 0.0, 1.0);
            vColor = aColor;
        }
        """;
    private static final String FS = """
        #version 330 core
        in vec4 vColor;
        out vec4 fragColor;
        void main() { fragColor = vColor; }
        """;

    private final ShaderProgram shader = new ShaderProgram(VS, FS);
    private final int vao, vbo, ebo;
    private final ByteBuffer batch = MemoryUtil.memAlloc(MAX_QUADS * 64);
    private final ByteBuffer scratch = MemoryUtil.memAlloc(64 * 1024);
    private final ByteBuffer white = MemoryUtil.memAlloc(4);
    private int quads;
    private float screenW = 1, screenH = 1;

    public UIRenderer() {
        white.putInt(0, -1);                                         // RGBA = 255,255,255,255 (absolute put keeps position 0)
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_QUADS * 64, GL_STREAM_DRAW);   // re-filled every frame
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 16, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 16, 12L);           // normalized: 0..255 -> 0..1

        // Quads are drawn as two triangles: (0,1,2) (0,2,3). The index pattern is fixed, so build it once.
        IntBuffer ib = MemoryUtil.memAllocInt(MAX_QUADS * 6);
        for (int q = 0; q < MAX_QUADS; q++) {
            int b = q * 4;
            ib.put(b).put(b + 1).put(b + 2).put(b).put(b + 2).put(b + 3);
        }
        ib.flip();
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        MemoryUtil.memFree(ib);
        glBindVertexArray(0);
    }

    public void begin(float w, float h) {
        screenW = w; screenH = h;
        quads = 0;
        batch.clear();
    }

    private static byte b(float v) { return (byte) Math.round(Math.max(0f, Math.min(1f, v)) * 255f); }

    private void vertex(float x, float y, byte r, byte g, byte bl, byte a) {
        batch.putFloat(x).putFloat(y).putFloat(0f).put(r).put(g).put(bl).put(a);
    }

    public void rect(float x, float y, float w, float h, float r, float g, float bl, float a) {
        if (quads >= MAX_QUADS) return;
        byte rr = b(r), gg = b(g), bb = b(bl), aa = b(a);
        vertex(x, y, rr, gg, bb, aa);
        vertex(x + w, y, rr, gg, bb, aa);
        vertex(x + w, y + h, rr, gg, bb, aa);
        vertex(x, y + h, rr, gg, bb, aa);
        quads++;
    }

    public float textWidth(String s, float scale) { return STBEasyFont.stb_easy_font_width(s) * scale; }

    /** Text with a 1-unit drop shadow for readability on any background. */
    public void text(String s, float x, float y, float scale, float r, float g, float bl) {
        addText(s, x + scale * 0.6f, y + scale * 0.6f, scale, 0f, 0f, 0f, 0.8f);
        addText(s, x, y, scale, r, g, bl, 1f);
    }

    private void addText(String s, float x, float y, float scale, float r, float g, float bl, float a) {
        scratch.clear();
        int n = STBEasyFont.stb_easy_font_print(0f, 0f, s, white, scratch);    // n quads in scratch
        if (quads + n > MAX_QUADS) return;
        byte rr = b(r), gg = b(g), bb = b(bl), aa = b(a);
        for (int i = 0; i < n * 4; i++) {
            float vx = scratch.getFloat(i * 16), vy = scratch.getFloat(i * 16 + 4);
            vertex(x + vx * scale, y + vy * scale, rr, gg, bb, aa);            // re-scale & re-colour each vertex
        }
        quads += n;
    }

    public void end() {
        if (quads > 0) {
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            shader.bind();
            shader.setVec2("uScreen", screenW, screenH);
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            batch.flip();
            glBufferSubData(GL_ARRAY_BUFFER, 0L, batch);
            glDrawElements(GL_TRIANGLES, quads * 6, GL_UNSIGNED_INT, 0L);

            glDisable(GL_BLEND);
            glEnable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);
        }
        batch.clear();
    }

    @Override public void close() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
        shader.close();
        MemoryUtil.memFree(batch);
        MemoryUtil.memFree(scratch);
        MemoryUtil.memFree(white);
    }
}
