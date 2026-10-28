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
        else camera.configure(1.9f, 2.0f, 7f);
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
                if (input.pressed(GLFW_KEY_1)) startGame(World.Hero.HULK);
                else if (input.pressed(GLFW_KEY_2)) startGame(World.Hero.IRON_MAN);
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
        }

        if (input.mouseDown(GLFW_MOUSE_BUTTON_LEFT) || input.key(GLFW_KEY_J)) world.heroAttack(camera);
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
        ui.text(p.displayName() + "   HP " + (int) Math.ceil(p.health), bx, by - 28 * s, 2.5f * s, 1, 1, 1);
        if (p instanceof IronMan im && im.isFlying()) ui.text("FLIGHT ON", bx, by - 56 * s, 2.5f * s, 0.5f, 1, 1);

        float cx = w / 2f, cy = h / 2f;                                    // crosshair
        ui.rect(cx - 10 * s, cy - 1, 20 * s, 2, 1, 1, 1, 0.8f);
        ui.rect(cx - 1, cy - 10 * s, 2, 20 * s, 1, 1, 1, 0.8f);

        if (world.bannerTimer > 0f) center("WAVE " + world.wave, h * 0.2f, 7 * s, 1, 0.9f, 0.3f, w);
        if (p.invuln > 0.55f) ui.rect(0, 0, w, h, 1, 0, 0, 0.18f);         // damage flash
    }

    private void drawMenu(float w, float h, float s) {
        center("MARVEL AVENGERS 3D", h * 0.10f, 6.5f * s, 1, 0.25f, 0.25f, w);
        center("PLAYER NAME: " + (playerName.isEmpty() ? "_" : playerName) + "  (TYPE / BACKSPACE)", h * 0.18f, 2.8f * s, 0.4f, 0.9f, 1, w);
        center("CHOOSE YOUR HERO", h * 0.28f, 3.5f * s, 1, 1, 1, w);
        center("1 - HULK   (SMASH, DOUBLE JUMP, GROUND POUND)", h * 0.37f, 3 * s, 0.4f, 1, 0.4f, w);
        center("2 - IRON MAN   (REPULSORS, FLIGHT)", h * 0.44f, 3 * s, 1, 0.6f, 0.3f, w);

        center("WASD MOVE   MOUSE LOOK   SPACE JUMP / ASCEND   LMB OR J ATTACK", h * 0.54f, 2.2f * s, 0.9f, 0.9f, 0.9f, w);
        center("HULK: CTRL GROUND POUND     IRON MAN: F FLIGHT, SHIFT DESCEND", h * 0.59f, 2.2f * s, 0.9f, 0.9f, 0.9f, w);
        center("V 1ST / 3RD PERSON    SCROLL ZOOM    ESC PAUSE", h * 0.64f, 2.2f * s, 0.9f, 0.9f, 0.9f, w);

        center("HIGH SCORES", h * 0.72f, 3 * s, 1, 0.9f, 0.3f, w);
        int[] top = scores.top();
        for (int i = 0; i < top.length; i++)
            center((i + 1) + ".  " + scores.name(i) + "  " + top[i], h * (0.78f + i * 0.04f), 2.4f * s, 1, 1, 1, w);
    }
}
