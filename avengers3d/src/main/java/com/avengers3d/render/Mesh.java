package com.avengers3d.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Owns one VAO + VBOs + index buffer.
 *   VAO  = saved vertex-attribute "recipe" (which buffer feeds which shader input).
 *   VBO  = raw GPU memory holding vertex attributes.
 *   EBO  = indices, so shared vertices are stored once.
 * Layout: location 0 = position(vec3), 1 = uv(vec2), 2 = normal(vec3).
 * Must be created and closed on the GL thread.
 */
public final class Mesh implements AutoCloseable {
    private final int vao;
    private final int[] buffers = new int[4];
    private final int indexCount;

    public Mesh(MeshData d) {
        indexCount = d.indices().length;
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        buffers[0] = uploadAttribute(d.positions(), 0, 3);
        buffers[1] = uploadAttribute(d.uvs(), 1, 2);
        buffers[2] = uploadAttribute(d.normals(), 2, 3);

        IntBuffer ib = MemoryUtil.memAllocInt(indexCount);       // off-heap: invisible to the GC
        try {
            ib.put(d.indices()).flip();
            buffers[3] = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[3]);   // EBO binding is stored inside the VAO
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(ib);                              // GPU has its own copy now
        }
        glBindVertexArray(0);
    }

    private static int uploadAttribute(float[] data, int location, int components) {
        FloatBuffer fb = MemoryUtil.memAllocFloat(data.length);
        try {
            fb.put(data).flip();
            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);   // STATIC: written once, drawn many times
            glEnableVertexAttribArray(location);
            glVertexAttribPointer(location, components, GL_FLOAT, false, 0, 0L);
            return vbo;
        } finally {
            MemoryUtil.memFree(fb);
        }
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
    }

    @Override public void close() {
        for (int b : buffers) glDeleteBuffers(b);
        glDeleteVertexArrays(vao);
    }
}
