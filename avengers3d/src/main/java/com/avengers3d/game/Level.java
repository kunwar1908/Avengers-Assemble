package com.avengers3d.game;

import com.avengers3d.render.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One playable scene: look, geometry, enemy waves and story text. Built by Levels. */
public final class Level {
    public record Line(String speaker, String text) { }
    /** boss: 0 none, 1 Commander Vex, 2 Warden Omega. */
    public record WaveSpec(int troopers, int drones, int brutes, int boss) { }

    public final int id;
    public final String title, location, objective;
    public final Environment env = new Environment();
    public float half = 32f;                     // playable half-size (positions are clamped to it)
    public boolean voidLevel, endless;
    public int groundTex;
    public float groundScale = 6f;
    public float[] groundTint = {1f, 1f, 1f};
    public final List<float[]> spawns = new ArrayList<>();
    public float bossX, bossZ = -16f;
    public WaveSpec[] waves = {};
    public Line[] intro = {}, outro = {};
    public Consumer<World> builder = w -> { };
    public int music;

    public Level(int id, String title, String location, String objective) {
        this.id = id; this.title = title; this.location = location; this.objective = objective;
    }

    public void ringSpawns(float radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = i * 2 * Math.PI / count;
            spawns.add(new float[]{(float) Math.cos(a) * radius, (float) Math.sin(a) * radius});
        }
    }
}
