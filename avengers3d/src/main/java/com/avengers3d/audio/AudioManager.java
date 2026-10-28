package com.avengers3d.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * OpenAL playback: a pool of 16 sources for sound effects plus one looping music source.
 * If no audio device exists (headless / driver issue) every call silently becomes a no-op.
 */
public final class AudioManager implements AutoCloseable {
    public enum Sound { JUMP, SMASH, SHOOT, HIT, KILL, HURT, WAVE, GAMEOVER }

    private final Random rng = new Random();
    private final int[] buffers = new int[Sound.values().length];
    private final int[] sources = new int[16];
    private int musicBuffer, musicSource;
    private long device, context;
    private boolean enabled;

    public AudioManager() {
        try {
            device = alcOpenDevice((ByteBuffer) null);
            if (device == NULL) return;
            ALCCapabilities caps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            alcMakeContextCurrent(context);
            AL.createCapabilities(caps);

            load(Sound.JUMP,     Synth.tone(300, 600, 0.15f, 0.40f, Synth.Wave.SQUARE));
            load(Sound.SMASH,    Synth.mix(Synth.tone(110, 35, 0.40f, 0.90f, Synth.Wave.SINE),
                                           Synth.tone(0, 0, 0.25f, 0.50f, Synth.Wave.NOISE)));
            load(Sound.SHOOT,    Synth.tone(900, 200, 0.12f, 0.35f, Synth.Wave.SQUARE));
            load(Sound.HIT,      Synth.tone(220, 100, 0.08f, 0.50f, Synth.Wave.SQUARE));
            load(Sound.KILL,     Synth.tone(500, 100, 0.25f, 0.50f, Synth.Wave.SQUARE));
            load(Sound.HURT,     Synth.mix(Synth.tone(180, 60, 0.30f, 0.70f, Synth.Wave.SQUARE),
                                           Synth.tone(0, 0, 0.20f, 0.40f, Synth.Wave.NOISE)));
            load(Sound.WAVE,     Synth.tone(300, 600, 0.40f, 0.50f, Synth.Wave.SINE));
            load(Sound.GAMEOVER, Synth.tone(400, 60, 1.20f, 0.60f, Synth.Wave.SQUARE));

            for (int i = 0; i < sources.length; i++) sources[i] = alGenSources();
            musicBuffer = alGenBuffers();
            alBufferData(musicBuffer, AL_FORMAT_MONO16, Synth.music(), Synth.RATE);
            musicSource = alGenSources();
            alSourcei(musicSource, AL_BUFFER, musicBuffer);
            alSourcei(musicSource, AL_LOOPING, AL_TRUE);
            alSourcef(musicSource, AL_GAIN, 0.22f);
            enabled = true;
        } catch (Throwable t) {
            System.err.println("Audio disabled: " + t);
            enabled = false;
        }
    }

    private void load(Sound s, short[] pcm) {
        int buf = alGenBuffers();
        alBufferData(buf, AL_FORMAT_MONO16, pcm, Synth.RATE);
        buffers[s.ordinal()] = buf;
    }

    public void startMusic() { if (enabled) alSourcePlay(musicSource); }

    public void play(Sound s) {
        if (!enabled) return;
        for (int src : sources) {
            if (alGetSourcei(src, AL_SOURCE_STATE) != AL_PLAYING) {
                alSourcei(src, AL_BUFFER, buffers[s.ordinal()]);
                alSourcef(src, AL_GAIN, 0.7f);
                alSourcef(src, AL_PITCH, 0.95f + rng.nextFloat() * 0.1f);   // slight variation avoids machine-gun repetition
                alSourcePlay(src);
                return;
            }
        }                                                                   // all voices busy: drop the sound
    }

    @Override public void close() {
        if (!enabled) return;
        alSourceStop(musicSource);
        alDeleteSources(musicSource);
        alDeleteBuffers(musicBuffer);
        for (int s : sources) { alSourceStop(s); alDeleteSources(s); }
        for (int b : buffers) alDeleteBuffers(b);
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
