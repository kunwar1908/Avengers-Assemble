package com.avengers3d.render;

/** Everything that defines the look of a scene: sky, sun, ambient light, fog, post-processing and ambient particles. */
public final class Environment {
    public enum Ambient { NONE, DUST, EMBERS, SNOW, SPARKS }

    public float[] skyTop = {0.10f, 0.20f, 0.50f};
    public float[] skyHorizon = {0.70f, 0.80f, 0.95f};
    public float[] sunDir = {-0.4f, -0.8f, -0.3f};       // direction the light travels
    public float[] sunColor = {2.4f, 2.2f, 2.0f};        // linear HDR colour
    public float[] ambientSky = {0.30f, 0.35f, 0.45f};
    public float[] ambientGround = {0.12f, 0.10f, 0.10f};
    public float[] fog = {0.60f, 0.70f, 0.85f};
    public float fogDensity = 0.010f, stars, aurora, clouds = 0.6f;
    public float exposure = 1f, bloom = 0.8f, vignette = 0.6f, saturation = 1.05f;
    public Ambient ambient = Ambient.NONE;
}
