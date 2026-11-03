package com.avengers3d.scene;

/** Appearance description for a humanoid: colours (null = feature absent), proportions, glow and props. */
public final class Look {
    public enum Prop { NONE, HAMMER, SHIELD, BOW, PISTOLS, BLADE, CANNONS }

    public float scale = 1f, bulk = 1f;                 // height multiplier, body width multiplier
    public float[] suit = c(.5f, .5f, .5f), suit2 = c(.3f, .3f, .3f), skin = c(.9f, .72f, .58f);
    public float[] boot = c(.15f, .15f, .15f), accent = c(1f, 1f, 1f);
    public float[] forearm, shin, glove, helm, hair, cape, pad;     // optional overrides / accessories
    public boolean eyes, chestGlow, palmGlow, bootGlow, star, belt, quiver, spikes, bun;
    public Prop prop = Prop.NONE;

    public static float[] c(float r, float g, float b) { return new float[]{r, g, b}; }
}
