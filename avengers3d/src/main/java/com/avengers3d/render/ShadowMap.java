package com.avengers3d.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Directional-light shadow map. The scene is rendered once from the sun's point of view into a depth
 * texture; the scene shader later compares each pixel's light-space depth with that texture.
 * The orthographic box follows the player and is snapped to whole texels, which stops shadow edges shimmering.
 */
public final class ShadowMap implements AutoCloseable {
    public static final int SIZE = 2048;
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    private final int fbo, depthTex;
    private final Matrix4f view = new Matrix4f(), proj = new Matrix4f(), lightVP = new Matrix4f();
    private final Vector3f right = new Vector3f(), upv = new Vector3f(), snapped = new Vector3f(), eye = new Vector3f();

    public ShadowMap() {
        depthTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, SIZE, SIZE, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1f, 1f, 1f, 1f});   // outside = lit
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);     // sampler2DShadow
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTex, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Shadow framebuffer incomplete");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Fit the light's orthographic box around {@code focus}. {@code lightDir} is the direction the light travels. */
    public Matrix4f update(Vector3f focus, float[] lightDir, float half) {
        Vector3f ld = new Vector3f(lightDir[0], lightDir[1], lightDir[2]).normalize();   // once per frame
        float texel = 2f * half / SIZE;
        ld.cross(UP, right).normalize();           // light-space right
        right.cross(ld, upv).normalize();          // light-space up
        float fx = focus.dot(right), fy = focus.dot(upv);
        float sx = (float) Math.floor(fx / texel) * texel, sy = (float) Math.floor(fy / texel) * texel;
        snapped.set(focus).fma(sx - fx, right).fma(sy - fy, upv);
        eye.set(snapped).fma(-90f, ld);
        view.setLookAt(eye, snapped, UP);
        proj.setOrtho(-half, half, -half, half, 1f, 200f);
        proj.mul(view, lightVP);
        return lightVP;
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, SIZE, SIZE);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public int texture() { return depthTex; }
    public Matrix4f lightVP() { return lightVP; }

    @Override public void close() { glDeleteFramebuffers(fbo); glDeleteTextures(depthTex); }
}
