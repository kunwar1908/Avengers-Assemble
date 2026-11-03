package com.avengers3d.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/** Texture upload helpers. Convention: RGB = colour, ALPHA = emissive mask (glows and feeds the bloom). */
public final class Textures {
    private Textures() { }

    public static int fromBytes(int w, int h, ByteBuffer px) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, px);
        glGenerateMipmap(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        return id;
    }

    /** 1x1 solid colour with NO emissive: lets every object share one shader path (texture * tint). */
    public static int solid(int r, int g, int b) {
        ByteBuffer px = MemoryUtil.memAlloc(4);
        try {
            px.put((byte) r).put((byte) g).put((byte) b).put((byte) 0).flip();
            return fromBytes(1, 1, px);
        } finally { MemoryUtil.memFree(px); }
    }
}
