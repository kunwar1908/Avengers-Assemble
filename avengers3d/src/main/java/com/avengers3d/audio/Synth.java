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
            if (i < 100) env *= i / 100f;
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

    /** Play several tones one after another (jingles). */
    public static short[] seq(float[] freqs, float noteDur, float vol, Wave wave) {
        int per = (int) (RATE * noteDur);
        short[] out = new short[per * freqs.length];
        for (int i = 0; i < freqs.length; i++) System.arraycopy(tone(freqs[i], freqs[i], noteDur, vol, wave), 0, out, i * per, per);
        return out;
    }

    /**
     * Looping arpeggio. {@code step} seconds per note; {@code squareMix} 0..1 adds buzz; {@code kick} adds a thump on every 2nd step.
     * A bass note an octave down is held on every 4th step.
     */
    public static short[] music(float[] arp, float step, float squareMix, boolean kick, float vol) {
        int stepN = (int) (RATE * step);
        float[] mix = new float[stepN * arp.length];
        for (int s = 0; s < arp.length; s++) {
            for (int i = 0; i < stepN; i++) {
                float t = i / (float) stepN;
                double ph = 2 * Math.PI * arp[s] * i / RATE;
                float sine = (float) Math.sin(ph), sq = sine >= 0 ? .4f : -.4f;
                mix[s * stepN + i] += (sine * (1f - squareMix) + sq * squareMix) * (float) Math.pow(1f - t, 1.5) * .26f * vol;
            }
            if (s % 4 == 0)
                for (int i = 0; i < stepN * 4 && s * stepN + i < mix.length; i++)
                    mix[s * stepN + i] += .3f * vol * (1f - i / (4f * stepN)) * (float) Math.sin(2 * Math.PI * (arp[s] / 2f) * i / RATE);
            if (kick && s % 2 == 0)
                for (int i = 0; i < stepN / 2; i++) {
                    float t = i / (stepN / 2f);
                    mix[s * stepN + i] += .5f * vol * (1f - t) * (float) Math.sin(2 * Math.PI * (60f - 25f * t) * i / RATE);
                }
        }
        short[] out = new short[mix.length];
        for (int i = 0; i < out.length; i++) out[i] = clip(mix[i]);
        return out;
    }

    private static short clip(float v) { return (short) (Math.max(-1f, Math.min(1f, v)) * 32767); }
}
