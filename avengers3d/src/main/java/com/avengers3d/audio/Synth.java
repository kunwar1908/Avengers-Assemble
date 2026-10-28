package com.avengers3d.audio;

import java.util.Random;

/** Procedural sound effects and music (16-bit mono PCM). No audio files required. */
public final class Synth {
    public static final int RATE = 22050;
    public enum Wave { SINE, SQUARE, NOISE }

    private static final Random RNG = new Random(7);
    private Synth() { }

    /** Frequency sweep f0 -> f1 with a quick attack and quadratic decay. */
    public static short[] tone(float f0, float f1, float dur, float vol, Wave wave) {
        int n = (int) (RATE * dur);
        short[] out = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            phase += 2 * Math.PI * (f0 + (f1 - f0) * t) / RATE;
            float s = switch (wave) {
                case SINE -> (float) Math.sin(phase);
                case SQUARE -> Math.sin(phase) >= 0 ? 0.6f : -0.6f;
                case NOISE -> RNG.nextFloat() * 2f - 1f;
            };
            float env = (1f - t) * (1f - t);
            if (i < 100) env *= i / 100f;                      // 4.5 ms fade-in removes the start click
            out[i] = clip(s * env * vol);
        }
        return out;
    }

    public static short[] mix(short[] a, short[] b) {
        short[] out = new short[Math.max(a.length, b.length)];
        for (int i = 0; i < out.length; i++) {
            int v = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
            out[i] = (short) Math.max(-32767, Math.min(32767, v));
        }
        return out;
    }

    /** ~3.5 s looping minor-key arpeggio with a bass note every 4 steps. */
    public static short[] music() {
        float[] arp = {220f, 261.63f, 329.63f, 392f, 220f, 261.63f, 329.63f, 392f,
                       196f, 246.94f, 293.66f, 392f, 174.61f, 220f, 261.63f, 349.23f};
        int stepN = (int) (RATE * 0.22f);
        float[] mix = new float[stepN * arp.length];
        for (int s = 0; s < arp.length; s++) {
            for (int i = 0; i < stepN; i++) {
                float t = i / (float) stepN;
                double ph = 2 * Math.PI * arp[s] * i / RATE;
                float tone = (float) Math.sin(ph) * 0.6f + (Math.sin(ph) >= 0 ? 0.4f : -0.4f) * 0.4f;
                mix[s * stepN + i] += tone * (float) Math.pow(1f - t, 1.5) * 0.25f;
            }
            if (s % 4 == 0) {
                for (int i = 0; i < stepN * 4 && s * stepN + i < mix.length; i++) {
                    float env = 1f - i / (4f * stepN);
                    mix[s * stepN + i] += 0.3f * env * (float) Math.sin(2 * Math.PI * (arp[s] / 2f) * i / RATE);
                }
            }
        }
        short[] out = new short[mix.length];
        for (int i = 0; i < out.length; i++) out[i] = clip(mix[i]);
        return out;
    }

    private static short clip(float v) { return (short) (Math.max(-1f, Math.min(1f, v)) * 32767); }
}
