package com.avengers3d.game;

import com.avengers3d.audio.AudioManager;
import com.avengers3d.audio.AudioManager.Sound;
import com.avengers3d.core.GameLoop;
import com.avengers3d.core.Input;
import com.avengers3d.core.Window;
import com.avengers3d.render.Camera3D;
import com.avengers3d.render.Mesh;
import com.avengers3d.render.Renderer;
import com.avengers3d.render.ShaderProgram;
import com.avengers3d.render.Textures;
import com.avengers3d.render.UIRenderer;
import com.avengers3d.scene.Character3D;
import com.avengers3d.scene.Hulk;
import com.avengers3d.scene.IronMan;
import com.avengers3d.scene.Thor;
import com.avengers3d.scene.CaptainAmerica;
import com.avengers3d.scene.BlackWidow;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;

/** Window + state machine (MENU -> PLAYING <-> PAUSED -> GAME_OVER -> MENU), HUD and input mapping. */
public final class GameApp implements GameLoop.Game {
    private enum State { MENU, PLAYING, PAUSED, GAME_OVER }

    private static final Vector3f ORIGIN = new Vector3f();

    private Window window;
    private final Input input = new Input();
    private final Camera3D camera = new Camera3D();
    private ShaderProgram shader;
    private Renderer renderer;
    private UIRenderer ui;
    private AudioManager audio;
    private World world;
    private final ScoreBoard scores = new ScoreBoard();
    private final List<Mesh> meshes = new ArrayList<>();

    private State state = State.MENU;
    private int lastRank = -1;
    private String playerName = "";
    private final Vector3f fwd = new Vector3f(), rgt = new Vector3f(), move = new Vector3f();

    // ---- setup / teardown -------------------------------------------------
    public void init() {
        window = new Window("Marvel Avengers 3D", 1280, 720);
        input.attach(window.handle());
        glClearColor(0.45f, 0.65f, 0.9f, 1f);
        glViewport(0, 0, window.width(), window.height());

        shader = ShaderProgram.fromResources("/shaders/basic.vert", "/shaders/basic.frag");
        renderer = new Renderer(shader);
        ui = new UIRenderer();
        audio = new AudioManager();

        Mesh cube = new Mesh(MeshFactory.cube());
        Mesh sphere = new Mesh(MeshFactory.sphere(12, 18));
        Mesh plane = new Mesh(MeshFactory.plane(60f, 30f));
        meshes.add(cube); meshes.add(sphere); meshes.add(plane);

        int white = Textures.solid(255, 255, 255);
        int checker = Textures.checker(64, 8, 0x4E8F4E, 0x3F7A3F);
        world = new World(cube, sphere, plane, white, checker, audio);

        scores.load();
        camera.setLens((float) Math.toRadians(70), window.aspect(), 0.1f, 300f);
        showMenuCamera();
        audio.startMusic();
    }

    public void shutdown() {
        if (audio != null) audio.close();
        if (ui != null) ui.close();
        if (shader != null) shader.close();
        for (Mesh m : meshes) m.close();
        if (window != null) window.close();
    }

    @Override public boolean shouldClose() { return window.shouldClose(); }

    // ---- state helpers ----------------------------------------------------
    private void capture(boolean captured) {
        window.setCursorCaptured(captured);
        input.resetMouse();
    }

    private void showMenuCamera() {
        camera.configure(1.7f, 2f, 28f);
        camera.setAngles(0f, -0.35f);
        capture(false);
        state = State.MENU;
    }

    private void startGame(World.Hero hero) {
        world.start(hero);
        if (hero == World.Hero.HULK) camera.configure(2.9f, 3.0f, 10f);
        else if (hero == World.Hero.IRON_MAN) camera.configure(1.9f, 2.0f, 7f);
        else camera.configure(2.1f, 2.2f, 8f);
        camera.setAngles(0f, -0.2f);
        capture(true);
        state = State.PLAYING;
    }

    private void gameOver() {
        lastRank = scores.submit(world.score, playerName);
        audio.play(Sound.GAMEOVER);
        capture(false);
        state = State.GAME_OVER;
    }

    // ---- input (once per frame) -------------------------------------------
    @Override public void input() {
        window.poll();
        if (window.consumeResize()) {
            glViewport(0, 0, window.width(), window.height());
            camera.setAspect(window.width(), window.height());
        }
        switch (state) {
            case MENU -> {
                updatePlayerName();
                World.Hero selected = selectedHero();
                if (selected != null) startGame(selected);
                else if (input.pressed(GLFW_KEY_ESCAPE)) window.requestClose();
            }
            case PLAYING -> playingInput();
            case PAUSED -> {
                if (input.pressed(GLFW_KEY_ESCAPE)) { state = State.PLAYING; capture(true); }
                else if (input.pressed(GLFW_KEY_Q)) { world.stop(); showMenuCamera(); }
            }
            case GAME_OVER -> {
                if (input.pressed(GLFW_KEY_ENTER)) { world.stop(); showMenuCamera(); }
            }
        }
        input.endFrame();
    }

    private void updatePlayerName() {
        if (input.pressed(GLFW_KEY_BACKSPACE) && !playerName.isEmpty())
            playerName = playerName.substring(0, playerName.length() - 1);
        String typed = input.text();
        if (!typed.isEmpty() && playerName.length() < 16)
            playerName = (playerName + typed).substring(0, Math.min(16, playerName.length() + typed.length()));
    }

    private float axis(int plus, int minus) { return (input.key(plus) ? 1f : 0f) - (input.key(minus) ? 1f : 0f); }

    private void playingInput() {
        if (input.pressed(GLFW_KEY_ESCAPE)) { state = State.PAUSED; capture(false); return; }
        Character3D p = world.player;
        if (input.pressed(GLFW_KEY_Q)) world.heroSpecial(camera);

        camera.rotate((float) input.mouseDX(), (float) input.mouseDY(), 0.0025f);
        camera.zoom((float) input.scroll());
        if (input.pressed(GLFW_KEY_V)) camera.toggleMode();

        // WASD is relative to where the camera looks (on the ground plane).
        camera.flatForward(fwd);
        camera.flatRight(rgt);
        move.set(0, 0, 0).fma(axis(GLFW_KEY_W, GLFW_KEY_S), fwd).fma(axis(GLFW_KEY_D, GLFW_KEY_A), rgt);
        if (move.lengthSquared() > 1f) move.normalize();
        p.moveInput.set(move);

        if (p instanceof Hulk h) {
            if (input.pressed(GLFW_KEY_SPACE) && h.jump()) audio.play(Sound.JUMP);
            if (input.pressed(GLFW_KEY_LEFT_CONTROL)) h.groundPound();
        } else if (p instanceof IronMan im) {
            if (input.pressed(GLFW_KEY_F)) { im.toggleFlight(); audio.play(Sound.JUMP); }
            if (im.isFlying()) im.setVerticalInput(axis(GLFW_KEY_SPACE, GLFW_KEY_LEFT_SHIFT));
            else if (input.pressed(GLFW_KEY_SPACE) && im.jump()) audio.play(Sound.JUMP);
        } else if (input.pressed(GLFW_KEY_SPACE) && p.grounded) {
            p.velocity.y = 11f;
            audio.play(Sound.JUMP);
        }

        if (input.mouseDown(GLFW_MOUSE_BUTTON_LEFT) || input.key(GLFW_KEY_J)) world.heroAttack(camera);
    }

    private World.Hero selectedHero() {
        if (input.pressed(GLFW_KEY_1)) return World.Hero.HULK;
        if (input.pressed(GLFW_KEY_2)) return World.Hero.IRON_MAN;
        if (input.pressed(GLFW_KEY_3)) return World.Hero.THOR;
        if (input.pressed(GLFW_KEY_4)) return World.Hero.CAPTAIN_AMERICA;
        if (input.pressed(GLFW_KEY_5)) return World.Hero.BLACK_WIDOW;
        if (input.pressed(GLFW_KEY_6)) return World.Hero.HAWKEYE;
        if (input.pressed(GLFW_KEY_7)) return World.Hero.WEB_RUNNER;
        if (input.pressed(GLFW_KEY_8)) return World.Hero.MYSTIC_WEAVER;
        if (input.pressed(GLFW_KEY_9)) return World.Hero.VISIONARY;
        if (input.pressed(GLFW_KEY_0)) return World.Hero.PANTHER;
        if (input.pressed(GLFW_KEY_A)) return World.Hero.SIZE_SHIFTER;
        if (input.pressed(GLFW_KEY_B)) return World.Hero.WINTER_SOLDIER;
        if (input.pressed(GLFW_KEY_C)) return World.Hero.ARCANE_SAGE;
        if (input.pressed(GLFW_KEY_D)) return World.Hero.SOLAR_VANGUARD;
        return null;
    }

    // ---- simulation -------------------------------------------------------
    @Override public void update(float dt) {
        switch (state) {
            case MENU -> camera.rotate(dt * 40f, 0f, 0.005f);          // slow fly-around behind the menu
            case PLAYING -> {
                world.update(dt);
                if (world.player.health <= 0f) gameOver();
            }
            default -> { }
        }
    }

    // ---- rendering --------------------------------------------------------
    @Override public void render(float alpha) {
        // NOTE: 'alpha' could interpolate transforms between physics steps; 60 Hz + vsync looks fine without.
        camera.follow(world.player != null ? world.player.transform().position() : ORIGIN);
        camera.update();
        renderer.render(camera, world.drawables());
        drawUi();
        window.swap();
    }

    private void center(String text, float y, float scale, float r, float g, float b, float w) {
        ui.text(text, (w - ui.textWidth(text, scale)) / 2f, y, scale, r, g, b);
    }

    private void drawUi() {
        float w = window.width(), h = window.height(), s = h / 720f;
        ui.begin(w, h);
        switch (state) {
            case MENU -> drawMenu(w, h, s);
            case PLAYING -> drawHud(w, h, s);
            case PAUSED -> {
                drawHud(w, h, s);
                ui.rect(0, 0, w, h, 0, 0, 0, 0.5f);
                center("PAUSED", h * 0.35f, 7 * s, 1, 1, 1, w);
                center("ESC - RESUME      Q - QUIT TO MENU", h * 0.5f, 3 * s, 1, 1, 1, w);
            }
            case GAME_OVER -> {
                drawHud(w, h, s);
                ui.rect(0, 0, w, h, 0.3f, 0, 0, 0.6f);
                center("GAME OVER", h * 0.25f, 8 * s, 1, 0.2f, 0.2f, w);
                center("SCORE: " + world.score, h * 0.42f, 4 * s, 1, 1, 1, w);
                center("KILLS: " + world.kills + "   WAVE: " + world.wave, h * 0.52f, 3 * s, 1, 1, 1, w);
                if (lastRank >= 0) center("NEW HIGH SCORE!  RANK #" + (lastRank + 1), h * 0.62f, 3.5f * s, 1, 0.9f, 0.2f, w);
                center("PRESS ENTER", h * 0.78f, 3 * s, 0.8f, 0.8f, 0.8f, w);
            }
        }
        ui.end();
    }

    private void drawHud(float w, float h, float s) {
        Character3D p = world.player;
        ui.text("SCORE: " + world.score, 20 * s, 16 * s, 3 * s, 1, 1, 1);
        ui.text("WAVE: " + world.wave, 20 * s, 50 * s, 2.5f * s, 1, 0.9f, 0.3f);
        ui.text("ENEMIES: " + world.enemies.size(), 20 * s, 78 * s, 2.5f * s, 1, 1, 1);
        ui.text("BEST: " + Math.max(scores.top()[0], world.score), w - 220 * s, 16 * s, 2.5f * s, 0.8f, 0.8f, 1);

        float bw = 320 * s, bh = 24 * s, bx = 20 * s, by = h - 50 * s;
        float frac = Math.max(0f, p.health / p.maxHealth);
        ui.rect(bx - 3, by - 3, bw + 6, bh + 6, 0, 0, 0, 0.6f);
        ui.rect(bx, by, bw * frac, bh, 1f - frac, frac, 0.1f, 1f);        // red -> green by health
        float[] nameColor = heroColor(p);
        ui.text(p.displayName(), bx, by - 28 * s, 2.5f * s, nameColor[0], nameColor[1], nameColor[2]);
        ui.text("   HP " + (int) Math.ceil(p.health),
                bx + ui.textWidth(p.displayName(), 2.5f * s), by - 28 * s, 2.5f * s, 1, 1, 1);
        ui.text("SPECIAL [Q]: " + (p.specialCooldown <= 0f ? "READY" : String.format("%.1fs", p.specialCooldown)),
                bx, by - 84 * s, 2.5f * s, 0.4f, 0.9f, 1);
        if (p instanceof IronMan im && im.isFlying()) ui.text("FLIGHT ON", bx, by - 56 * s, 2.5f * s, 0.5f, 1, 1);

        float cx = w / 2f, cy = h / 2f;                                    // crosshair
        ui.rect(cx - 10 * s, cy - 1, 20 * s, 2, 1, 1, 1, 0.8f);
        ui.rect(cx - 1, cy - 10 * s, 2, 20 * s, 1, 1, 1, 0.8f);

        if (world.bannerTimer > 0f) center("WAVE " + world.wave, h * 0.2f, 7 * s, 1, 0.9f, 0.3f, w);
        if (p.invuln > 0.55f) ui.rect(0, 0, w, h, 1, 0, 0, 0.18f);         // damage flash
    }

    private void drawMenu(float w, float h, float s) {
        ui.rect(0, 0, w, h, 0.015f, 0.025f, 0.08f, 0.35f);
        center("MARVEL AVENGERS 3D", h * 0.055f, 6 * s, 1, 0.3f, 0.35f, w);
        center("ASSEMBLE YOUR HERO", h * 0.135f, 3 * s, 0.65f, 0.85f, 1, w);

        float nameY = h * 0.19f;
        ui.rect(w * 0.25f, nameY - 5 * s, w * 0.5f, 34 * s, 0.04f, 0.12f, 0.22f, 0.9f);
        center("PLAYER: " + (playerName.isEmpty() ? "_" : playerName) + "   |   TYPE TO EDIT", nameY, 2.4f * s, 0.4f, 0.95f, 1, w);

        float panelY = h * 0.25f, panelH = h * 0.48f;
        ui.rect(w * 0.06f, panelY, w * 0.41f, panelH, 0.04f, 0.06f, 0.14f, 0.92f);
        ui.rect(w * 0.53f, panelY, w * 0.41f, panelH, 0.04f, 0.06f, 0.14f, 0.92f);
        ui.text("HEROES", w * 0.09f, panelY + 25 * s, 2.5f * s, 1, 0.85f, 0.25f);
        ui.text("HEROES", w * 0.56f, panelY + 25 * s, 2.5f * s, 1, 0.85f, 0.25f);

        menuHero("1  HULK", "SMASH", w * 0.09f, panelY + 62 * s, 0.3f, 1, 0.3f, s);
        menuHero("2  IRON MAN", "REPULSORS / FLIGHT", w * 0.09f, panelY + 98 * s, 1, 0.55f, 0.15f, s);
        menuHero("3  THOR", "LIGHTNING", w * 0.09f, panelY + 134 * s, 0.4f, 0.7f, 1, s);
        menuHero("4  CAPTAIN AMERICA", "SHIELD", w * 0.09f, panelY + 170 * s, 0.25f, 0.55f, 1, s);
        menuHero("5  BLACK WIDOW", "DUAL SHOTS", w * 0.09f, panelY + 206 * s, 0.95f, 0.25f, 0.35f, s);
        menuHero("6  HAWKEYE", "PRECISION", w * 0.09f, panelY + 242 * s, 0.75f, 0.35f, 0.9f, s);

        menuHero("7  SPIDER-MAN", "WEB STRIKE", w * 0.56f, panelY + 62 * s, 0.95f, 0.25f, 0.7f, s);
        menuHero("8  SCARLET WITCH", "MYSTIC BLAST", w * 0.56f, panelY + 98 * s, 0.8f, 0.25f, 1, s);
        menuHero("9  VISION", "HEAL / ENERGY", w * 0.56f, panelY + 134 * s, 0.95f, 0.95f, 0.65f, s);
        menuHero("0  BLACK PANTHER", "AMBUSH", w * 0.56f, panelY + 170 * s, 0.55f, 0.55f, 0.65f, s);
        menuHero("A  ANT-MAN", "HEAVY STRIKE", w * 0.56f, panelY + 206 * s, 0.9f, 0.65f, 0.2f, s);
        menuHero("B  WINTER SOLDIER", "VOLLEY", w * 0.56f, panelY + 242 * s, 0.35f, 0.65f, 0.95f, s);
        menuHero("C  DOCTOR STRANGE", "AREA SPELL", w * 0.56f, panelY + 278 * s, 0.55f, 0.25f, 1, s);
        menuHero("D  CAPTAIN MARVEL", "SOLAR BLAST", w * 0.56f, panelY + 314 * s, 1, 0.45f, 0.1f, s);

        ui.rect(w * 0.06f, h * 0.77f, w * 0.88f, h * 0.17f, 0.04f, 0.06f, 0.14f, 0.92f);
        center("WASD MOVE   |   MOUSE LOOK   |   SPACE JUMP   |   LMB / J ATTACK   |   Q SPECIAL", h * 0.80f, 2.2f * s, 0.85f, 0.9f, 1, w);
        center("V CAMERA   |   SCROLL ZOOM   |   ESC PAUSE     HULK: CTRL POUND     IRON MAN: F FLIGHT", h * 0.845f, 2.1f * s, 0.7f, 0.75f, 0.85f, w);
        center("TOP SCORES", h * 0.895f, 2.2f * s, 1, 0.85f, 0.25f, w);
        int[] top = scores.top();
        center("TOP: " + scores.name(0) + " " + top[0] + "   " + scores.name(1) + " " + top[1]
                + "   " + scores.name(2) + " " + top[2], h * 0.915f, 1.6f * s, 1, 1, 1, w);
    }

    private void menuHero(String name, String power, float x, float y, float r, float g, float b, float s) {
        ui.text(name, x, y, 2.4f * s, r, g, b);
        ui.text("  " + power, x + ui.textWidth(name, 2.4f * s) + 10 * s, y,
                1.65f * s, 0.65f, 0.72f, 0.85f);
    }

    private void coloredMenuLine(String[] labels, float y, float scale, float width, float[][] colors) {
        float total = 0f;
        for (String label : labels) total += ui.textWidth(label, scale);
        float x = (width - total) / 2f;
        for (int i = 0; i < labels.length; i++) {
            float[] color = colors[i];
            ui.text(labels[i], x, y, scale, color[0], color[1], color[2]);
            x += ui.textWidth(labels[i], scale);
        }
    }

    private float[] heroColor(Character3D hero) {
        if (hero instanceof Hulk) return new float[]{0.3f, 1f, 0.3f};
        if (hero instanceof IronMan) return new float[]{1f, 0.55f, 0.15f};
        if (hero instanceof Thor) return new float[]{0.4f, 0.7f, 1f};
        if (hero instanceof CaptainAmerica) return new float[]{0.25f, 0.55f, 1f};
        if (hero instanceof BlackWidow) return new float[]{0.95f, 0.25f, 0.35f};
        return new float[]{0.8f, 0.8f, 1f};
    }
}
