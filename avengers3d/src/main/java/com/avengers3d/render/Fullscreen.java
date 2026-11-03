package com.avengers3d.render;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL30.*;

/** A single oversized triangle generated from gl_VertexID: needs no vertex buffer, only an (empty) VAO. */
public final class Fullscreen implements AutoCloseable {
    private final int vao = glGenVertexArrays();

    public void draw() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    @Override public void close() { glDeleteVertexArrays(vao); }
}
