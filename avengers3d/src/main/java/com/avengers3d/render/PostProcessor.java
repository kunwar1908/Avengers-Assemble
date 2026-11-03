package com.avengers3d.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

/**
 * HDR post pipeline:
 *   scene -> 4x MSAA float framebuffer -> resolve to a texture -> bright-pass -> Gaussian blur (1/2 and 1/4 res)
 *   -> composite (bloom add, ACES tone mapping, gamma, vignette, damage/heal tint, fade) on the screen.
 * Lighting happens in linear HDR space, so emissive surfaces and sparks can exceed 1.0 and "bloom".
 */
public final class PostProcessor implements AutoCloseable {
    public static final class Params {
        public float exposure = 1f, bloom = 0.8f, fade = 0f, vignette = 0.6f, saturation = 1.05f;
        public float overlayR, overlayG, overlayB, overlayA;
    }

    private int w, h;
    private int msFbo, msColor, msDepth, hdrFbo, hdrTex;
    private final int[] bFbo = new int[4], bTex = new int[4];      // [0],[1] half res   [2],[3] quarter res
    private final ShaderProgram bright = ShaderProgram.load("fullscreen.vert", "bright.frag");
    private final ShaderProgram blur = ShaderProgram.load("fullscreen.vert", "blur.frag");
    private final ShaderProgram composite = ShaderProgram.load("fullscreen.vert", "composite.frag");
    private final Fullscreen quad = new Fullscreen();

    private static int hdrTexture(int width, int height) {
        int t = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, t);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return t;
    }

    private static int fboWith(int tex) {
        int f = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, f);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Post framebuffer incomplete");
        return f;
    }

    private void release() {
        if (msFbo == 0) return;
        glDeleteFramebuffers(msFbo); glDeleteRenderbuffers(msColor); glDeleteRenderbuffers(msDepth);
        glDeleteFramebuffers(hdrFbo); glDeleteTextures(hdrTex);
        for (int i = 0; i < 4; i++) { glDeleteFramebuffers(bFbo[i]); glDeleteTextures(bTex[i]); }
    }

    private void resize(int width, int height) {
        release();
        w = Math.max(2, width); h = Math.max(2, height);

        msFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, msFbo);
        msColor = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, msColor);
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, 4, GL_RGBA16F, w, h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, msColor);
        msDepth = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, msDepth);
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, 4, GL_DEPTH_COMPONENT24, w, h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, msDepth);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("MSAA framebuffer incomplete");

        hdrTex = hdrTexture(w, h);
        hdrFbo = fboWith(hdrTex);
        for (int i = 0; i < 4; i++) {
            int div = i < 2 ? 2 : 4;
            bTex[i] = hdrTexture(Math.max(1, w / div), Math.max(1, h / div));
            bFbo[i] = fboWith(bTex[i]);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Start rendering the scene into the HDR target. */
    public void begin(int width, int height) {
        if (width != w || height != h || msFbo == 0) resize(width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, msFbo);
        glViewport(0, 0, w, h);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void pass(int fbo, int vw, int vh) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, vw, vh);
        quad.draw();
    }

    private void blurPair(int a, int b, int vw, int vh) {
        blur.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, bTex[a]);
        blur.setInt("uImage", 0);
        blur.setVec2("uDir", 1f / vw, 0f);
        pass(bFbo[b], vw, vh);
        glBindTexture(GL_TEXTURE_2D, bTex[b]);
        blur.setVec2("uDir", 0f, 1f / vh);
        pass(bFbo[a], vw, vh);
    }

    /** Resolve MSAA, build the bloom, and draw the finished frame to the screen. */
    public void end(Params p) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, msFbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, hdrFbo);
        glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);

        int hw = Math.max(1, w / 2), hh = Math.max(1, h / 2), qw = Math.max(1, w / 4), qh = Math.max(1, h / 4);

        bright.bind();                                         // bright-pass: keep only HDR values above the threshold
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, hdrTex);
        bright.setInt("uScene", 0);
        bright.setVec2("uTexel", 1f / w, 1f / h);
        bright.setFloat("uThreshold", 1.05f);
        pass(bFbo[0], hw, hh);
        blurPair(0, 1, hw, hh);
        blurPair(0, 1, hw, hh);

        bright.bind();                                         // threshold 0 = plain downsample into the quarter-res level
        glBindTexture(GL_TEXTURE_2D, bTex[0]);
        bright.setVec2("uTexel", 1f / hw, 1f / hh);
        bright.setFloat("uThreshold", 0f);
        pass(bFbo[2], qw, qh);
        blurPair(2, 3, qw, qh);
        blurPair(2, 3, qw, qh);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, w, h);
        composite.bind();
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, hdrTex);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, bTex[0]);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, bTex[2]);
        composite.setInt("uScene", 0); composite.setInt("uBloom0", 1); composite.setInt("uBloom1", 2);
        composite.setFloat("uExposure", p.exposure);
        composite.setFloat("uBloom", p.bloom);
        composite.setFloat("uFade", p.fade);
        composite.setFloat("uVignette", p.vignette);
        composite.setFloat("uSaturation", p.saturation);
        composite.setVec4("uOverlay", p.overlayR, p.overlayG, p.overlayB, p.overlayA);
        quad.draw();
        glActiveTexture(GL_TEXTURE0);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    @Override public void close() {
        release();
        bright.close(); blur.close(); composite.close(); quad.close();
    }
}
