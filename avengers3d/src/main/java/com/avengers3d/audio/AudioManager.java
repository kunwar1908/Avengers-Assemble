package com.avengers3d.audio;

import org.joml.Vector3f;
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
 * OpenAL playback: 24 pooled 3D sound-effect voices + one looping music source.
 * Positional sounds are attenuated by distance and panned relative to the listener (the camera).
 * If no audio device exists, every call silently becomes a no-op.
 */
public final class AudioManager implements AutoCloseable {
    public enum Sound {
        JUMP, DODGE, SMASH, SHOOT, BEAM, EXPLOSION, HIT, KILL, HURT, WAVE, GAMEOVER, ROAR, THUNDER, SHIELD, STUN,
        ENEMY_SHOOT, SLAM, WARNING, UI_MOVE, UI_SELECT, BLIP, VICTORY, ARROW, BOSS
    }

    private final Random rng = new Random();
    private final int[] buffers = new int[Sound.values().length];
    private final long[] lastPlayed = new long[Sound.values().length];
    private final int[] sources = new int[24];
    private final int[] musicBuffers = new int[5];
    private int musicSource, currentMusic = -1;
    private float musicVolume = .5f;
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
            alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);

            Synth.Wave sq = Synth.Wave.SQUARE, si = Synth.Wave.SINE, no = Synth.Wave.NOISE;
            load(Sound.JUMP, Synth.tone(300, 600, .15f, .40f, sq));
            load(Sound.DODGE, Synth.mix(Synth.tone(0, 0, .22f, .35f, no), Synth.tone(500, 200, .2f, .2f, si)));
            load(Sound.SMASH, Synth.mix(Synth.tone(110, 35, .40f, .90f, si), Synth.tone(0, 0, .25f, .50f, no)));
            load(Sound.SHOOT, Synth.tone(900, 200, .12f, .35f, sq));
            load(Sound.BEAM, Synth.mix(Synth.tone(200, 1400, .55f, .45f, si), Synth.tone(0, 0, .4f, .3f, no)));
            load(Sound.EXPLOSION, Synth.mix(Synth.tone(90, 28, .75f, .65f, si), Synth.tone(0, 0, .6f, .4f, no)));
            load(Sound.HIT, Synth.tone(220, 100, .08f, .50f, sq));
            load(Sound.KILL, Synth.tone(500, 100, .25f, .50f, sq));
            load(Sound.HURT, Synth.mix(Synth.tone(180, 60, .30f, .70f, sq), Synth.tone(0, 0, .20f, .40f, no)));
            load(Sound.WAVE, Synth.seq(new float[]{330, 440, 550}, .12f, .45f, si));
            load(Sound.GAMEOVER, Synth.tone(400, 60, 1.2f, .60f, sq));
            load(Sound.ROAR, Synth.mix(Synth.tone(70, 190, .9f, .7f, sq), Synth.tone(0, 0, .8f, .45f, no)));
            load(Sound.THUNDER, Synth.mix(Synth.tone(0, 0, 1.0f, .8f, no), Synth.tone(70, 30, 1.0f, .7f, si)));
            load(Sound.SHIELD, Synth.tone(1200, 500, .22f, .40f, si));
            load(Sound.STUN, Synth.tone(1500, 300, .5f, .40f, sq));
            load(Sound.ENEMY_SHOOT, Synth.tone(500, 150, .15f, .30f, sq));
            load(Sound.SLAM, Synth.mix(Synth.tone(70, 25, .55f, .7f, si), Synth.tone(0, 0, .35f, .4f, no)));
            load(Sound.WARNING, Synth.seq(new float[]{880, 660}, .09f, .35f, sq));
            load(Sound.UI_MOVE, Synth.tone(800, 900, .05f, .25f, si));
            load(Sound.UI_SELECT, Synth.tone(600, 1200, .12f, .35f, si));
            load(Sound.BLIP, Synth.tone(1100, 1100, .025f, .18f, sq));
            load(Sound.VICTORY, Synth.seq(new float[]{523, 659, 784, 1047}, .16f, .45f, si));
            load(Sound.ARROW, Synth.tone(1400, 300, .14f, .30f, si));
            load(Sound.BOSS, Synth.mix(Synth.tone(55, 30, 1.2f, .7f, sq), Synth.tone(0, 0, .9f, .35f, no)));

            for (int i = 0; i < sources.length; i++) {
                sources[i] = alGenSources();
                alSourcef(sources[i], AL_REFERENCE_DISTANCE, 7f);
                alSourcef(sources[i], AL_ROLLOFF_FACTOR, 1.1f);
                alSourcef(sources[i], AL_MAX_DISTANCE, 90f);
            }
            musicSource = alGenSources();
            alSourcei(musicSource, AL_LOOPING, AL_TRUE);
            alSourcei(musicSource, AL_SOURCE_RELATIVE, AL_TRUE);
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

    public void setListener(Vector3f pos, Vector3f forward, Vector3f up) {
        if (!enabled) return;
        alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListenerfv(AL_ORIENTATION, new float[]{forward.x, forward.y, forward.z, up.x, up.y, up.z});
    }

    /** Non-positional sound (UI, announcements). */
    public void play(Sound s) { play(s, 0, 0, 0, true); }
    public void play(Sound s, Vector3f at) { play(s, at.x, at.y, at.z, false); }
    public void play(Sound s, float x, float y, float z) { play(s, x, y, z, false); }

    private void play(Sound s, float x, float y, float z, boolean relative) {
        if (!enabled) return;
        long now = System.nanoTime();
        if (now - lastPlayed[s.ordinal()] < 35_000_000L) return;       // throttle: identical sounds closer than 35 ms are dropped
        lastPlayed[s.ordinal()] = now;
        for (int src : sources) {
            if (alGetSourcei(src, AL_SOURCE_STATE) != AL_PLAYING) {
                alSourcei(src, AL_BUFFER, buffers[s.ordinal()]);
                alSourcei(src, AL_SOURCE_RELATIVE, relative ? AL_TRUE : AL_FALSE);
                alSource3f(src, AL_POSITION, x, y, z);
                alSourcef(src, AL_GAIN, relative ? .8f : 1f);
                alSourcef(src, AL_PITCH, .94f + rng.nextFloat() * .12f);
                alSourcePlay(src);
                return;
            }
        }
    }

    /** 0 menu, 1 city, 2 foundry, 3 ridge, 4 boss/citadel. Buffers are generated on first use. */
    public void playMusic(int style) {
        if (!enabled || style < 0 || style >= musicBuffers.length || style == currentMusic) return;
        if (musicBuffers[style] == 0) {
            musicBuffers[style] = alGenBuffers();
            short[] pcm = switch (style) {
                case 0 -> Synth.music(new float[]{220f, 261.6f, 329.6f, 392f, 329.6f, 261.6f, 220f, 196f}, .30f, .05f, false, 1f);
                case 1 -> Synth.music(new float[]{220f, 261.63f, 329.63f, 392f, 220f, 261.63f, 329.63f, 392f,
                                                  196f, 246.94f, 293.66f, 392f, 174.61f, 220f, 261.63f, 349.23f}, .22f, .35f, true, 1f);
                case 2 -> Synth.music(new float[]{110f, 130.8f, 164.8f, 130.8f, 123.5f, 146.8f, 185f, 146.8f,
                                                  110f, 130.8f, 164.8f, 196f, 123.5f, 146.8f, 185f, 220f}, .20f, .6f, true, 1f);
                case 3 -> Synth.music(new float[]{261.6f, 329.6f, 392f, 523.3f, 392f, 329.6f, 261.6f, 196f}, .38f, 0f, false, .9f);
                default -> Synth.music(new float[]{146.8f, 174.6f, 220f, 174.6f, 130.8f, 155.6f, 196f, 155.6f,
                                                   146.8f, 174.6f, 220f, 261.6f, 130.8f, 155.6f, 196f, 233f}, .17f, .7f, true, 1.1f);
            };
            alBufferData(musicBuffers[style], AL_FORMAT_MONO16, pcm, Synth.RATE);
        }
        alSourceStop(musicSource);
        alSourcei(musicSource, AL_BUFFER, musicBuffers[style]);
        alSourcef(musicSource, AL_GAIN, musicVolume * .45f);
        alSourcePlay(musicSource);
        currentMusic = style;
    }

    public void setMusicVolume(float v) {
        musicVolume = Math.max(0f, Math.min(1f, v));
        if (enabled) alSourcef(musicSource, AL_GAIN, musicVolume * .45f);
    }
    public float musicVolume() { return musicVolume; }

    @Override public void close() {
        if (!enabled) return;
        alSourceStop(musicSource);
        alDeleteSources(musicSource);
        for (int b : musicBuffers) if (b != 0) alDeleteBuffers(b);
        for (int s : sources) { alSourceStop(s); alDeleteSources(s); }
        for (int b : buffers) alDeleteBuffers(b);
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
