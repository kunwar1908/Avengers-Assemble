package com.avengers3d.render;

import static org.lwjgl.opengl.GL11.*;

/** Procedural sky: gradient, sun, clouds, stars and aurora, evaluated per pixel from the view ray. */
public final class SkyRenderer implements AutoCloseable {
    private final ShaderProgram shader = ShaderProgram.load("fullscreen.vert", "sky.frag");
    private final Fullscreen quad = new Fullscreen();

    public void draw(Camera3D cam, Environment env, float time) {
        glDisable(GL_DEPTH_TEST);                   // the sky is behind everything: no depth test, no depth write
        glDisable(GL_CULL_FACE);
        shader.bind();
        shader.setMat4("uInvViewProj", cam.invViewProjection());
        shader.setVec3("uCamPos", cam.eye());
        shader.setVec3("uSkyTop", env.skyTop);
        shader.setVec3("uSkyHorizon", env.skyHorizon);
        shader.setVec3("uSunDir", env.sunDir);
        shader.setVec3("uSunColor", env.sunColor);
        shader.setVec3("uFogColor", env.fog);
        shader.setFloat("uStars", env.stars);
        shader.setFloat("uAurora", env.aurora);
        shader.setFloat("uClouds", env.clouds);
        shader.setFloat("uTime", time);
        quad.draw();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    @Override public void close() { shader.close(); quad.close(); }
}
