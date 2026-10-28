package com.avengers3d.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/** Procedurally generated textures (no image files needed). Call on the GL thread. */
public final class Textures {
    private Textures() { }

    /** 1x1 solid colour: lets every object use the same shader path (texture * tint). */
    public static int solid(int r, int g, int b) {
        ByteBuffer px = MemoryUtil.memAlloc(4);
        try {
            px.put((byte) r).put((byte) g).put((byte) b).put((byte) 255).flip();
            return upload(1, 1, px, true);
        } finally { MemoryUtil.memFree(px); }
    }

    /** size x size checkerboard with square cells of {@code cell} pixels, colours as 0xRRGGBB. */
    public static int checker(int size, int cell, int rgb1, int rgb2) {
        ByteBuffer px = MemoryUtil.memAlloc(size * size * 4);
        try {
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++) {
                    int c = (((x / cell) + (y / cell)) & 1) == 0 ? rgb1 : rgb2;
                    px.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) 255);
                }
            px.flip();
            return upload(size, size, px, true);
        } finally { MemoryUtil.memFree(px); }
    }

    private static int upload(int w, int h, ByteBuffer px, boolean nearestMag) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        glGenerateMipmap(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, nearestMag ? GL_NEAREST : GL_LINEAR);
        return id;
    }
}
