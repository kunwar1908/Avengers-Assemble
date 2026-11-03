package com.avengers3d.game;

import com.avengers3d.audio.AudioManager;
import com.avengers3d.audio.AudioManager.Sound;
import com.avengers3d.core.GameLoop;
import com.avengers3d.core.Input;
import com.avengers3d.core.Window;
import com.avengers3d.render.Camera3D;
import com.avengers3d.render.Environment;
import com.avengers3d.render.Lights;
import com.avengers3d.render.ParticleSystem;
import com.avengers3d.render.PostProcessor;
import com.avengers3d.render.Renderer;
import com.avengers3d.render.ShadowMap;
import com.avengers3d.render.UIRenderer;
import com.avengers3d.scene.GameObject3D;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

/** Window, state machine (menus, story dialogue, gameplay, pause, results), frame pipeline and UI screens. */
public final class GameApp implements GameLoop.Game {
    private enum State { MAIN_MENU, NAME_ENTRY, HERO_SELECT, CHAPTER_SELECT, CONTROLS, SCORES, DIALOGUE, PLAYING, PAUSED, LEVEL_COMPLETE, GAME_OVER, ENDING }

    private static final Vector3f ORIGIN = new Vector3f();
    private static final String[] CH_TITLE = {"STREET WAR", "THE FOUNDRY", "FROZEN RIDGE", "THE CITADEL"};
    private static final String[] CH_PLACE = {"DOWNTOWN", "EAST INDUSTRIAL", "NORTHERN RIDGE", "ORBITAL FORTRESS"};
    private static final String[] MAIN_ITEMS = {"STORY MODE", "ARENA SURVIVAL", "HIGH SCORES", "CONTROLS", "CHANGE NAME", "QUIT"};

    private Window window;
    private final Input input = new Input();
    private final Camera3D camera = new Camera3D();
    private Renderer renderer;
    private ShadowMap shadow;
    private PostProcessor post;
    private ParticleSystem particles;
    private UIRenderer ui;
    private Hud hud;
    private AudioManager audio;
    private Assets assets;
    private World world;
    private final SaveData save = new SaveData();
    private final PostProcessor.Params postParams = new PostProcessor.Params();
    private Level stageLevel;

    private State state = State.MAIN_MENU;
    private int menuIndex, heroIndex, chapter = 1, chapterSel = 1, lastRank = -1;
    private boolean storyMode;
    private HeroDef currentHero = Heroes.ALL[0];
    private String nameBuffer = "";
    private float time, fade = 1f, hitStopT, creditsT, completeGradeHp;
    private int fadeDir = -1;
    private Runnable pending;
    private long lastNanos = System.nanoTime();
    private String grade = "C";

    // dialogue
    private Level.Line[] lines = {};
    private int lineIdx;
    private float typeT;
    private Runnable onDialogueDone;

    private final Vector3f fwd = new Vector3f(), rgt = new Vector3f(), move = new Vector3f();

    // ======================= setup / teardown =======================
    public void init() {
        window = new Window("Marvel Avengers 3D", 1280, 720);
        input.attach(window.handle());
        glViewport(0, 0, window.width(), window.height());
        assets = new Assets();
        renderer = new Renderer();
        shadow = new ShadowMap();
        post = new PostProcessor();
        particles = new ParticleSystem(9000);
        ui = new UIRenderer();
        hud = new Hud(ui);
        audio = new AudioManager();
        world = new World(assets, particles, audio);
        save.load();
        camera.setLens((float) Math.toRadians(68), window.aspect(), .1f, 400f);
        stageLevel = Levels.stage(assets);
        enterMenuScene();
        state = save.playerName.isEmpty() ? State.NAME_ENTRY : State.MAIN_MENU;
        nameBuffer = save.playerName;
    }

    public void shutdown() {
        if (audio != null) audio.close();
        if (ui != null) ui.close();
        if (post != null) post.close();
        if (particles != null) particles.close();
        if (shadow != null) shadow.close();
        if (renderer != null) renderer.close();
        if (assets != null) assets.close();
        if (window != null) window.close();
    }

    @Override public boolean shouldClose() { return window.shouldClose(); }

    // ======================= scene / state helpers =======================
    private void capture(boolean captured) { window.setCursorCaptured(captured); input.resetMouse(); }

    private void transition(Runnable action) {
        if (pending != null) return;
        pending = action;
        fadeDir = 1;
    }

    private void enterMenuScene() {
        world.loadLevel(stageLevel, false);
        world.showcaseHero(Heroes.ALL[heroIndex]);
        particles.clear();
        audio.playMusic(0);
        camera.setShoulderOffset(-2.2f);
        camera.follow(ORIGIN);
        camera.snap();
        capture(false);
    }

    private void goMainMenu() {
        world.stop();
        enterMenuScene();
        state = State.MAIN_MENU;
        menuIndex = 0;
    }

    private void configureGameplayCamera() {
        float sc = currentHero.look.scale;
        camera.configure(1.7f * sc, 1.8f * sc, 6.5f + 3f * sc);
        camera.setShoulderOffset(.9f * Math.max(1f, sc * .8f));
        camera.setAngles(0f, -.2f);
    }

    private void beginChapter(int ch) {
        chapter = ch;
        Level lvl = Levels.chapter(ch, assets);
        world.loadLevel(lvl, true);
        world.startRun(currentHero);
        particles.clear();
        audio.playMusic(lvl.music);
        float sc = currentHero.look.scale;
        camera.configure(1.2f * sc, 1.1f * sc, 6f + 3f * sc);
        camera.setShoulderOffset(-1.4f);
        camera.follow(world.player.transform().position());
        camera.snap();
        startDialogue(lvl.intro, () -> transition(this::beginPlay));
        capture(false);
    }

    private void beginPlay() {
        world.startRun(currentHero);
        particles.clear();
        configureGameplayCamera();
        camera.follow(world.player.transform().position());
        camera.snap();
        state = State.PLAYING;
        capture(true);
    }

    private void beginArena() {
        storyMode = false;
        Level lvl = Levels.arena(assets);
        world.loadLevel(lvl, false);
        audio.playMusic(lvl.music);
        beginPlay();
    }

    private void retry() {
        if (storyMode) {
            world.loadLevel(Levels.chapter(chapter, assets), true);
            audio.playMusic(world.level.music);
            beginPlay();
        } else beginArena();
    }

    private void startDialogue(Level.Line[] ls, Runnable done) {
        lines = ls; lineIdx = 0; typeT = 0f; onDialogueDone = done;
        state = State.DIALOGUE;
    }

    private void toEnding() {
        world.stop();
        enterMenuScene();
        world.showcaseHero(currentHero);
        creditsT = 0f;
        state = State.ENDING;
    }

    private String speaker(String s) { return s.replace("{HERO}", currentHero.name); }

    // ======================= input =======================
    private int navigate(int count) {
        int d = 0;
        if (input.pressed(GLFW_KEY_DOWN) || input.pressed(GLFW_KEY_S)) d = 1;
        if (input.pressed(GLFW_KEY_UP) || input.pressed(GLFW_KEY_W)) d = -1;
        return d;
    }

    private boolean confirm() { return input.pressed(GLFW_KEY_ENTER) || input.pressed(GLFW_KEY_SPACE); }

    private float axis(int plus, int minus) { return (input.key(plus) ? 1f : 0f) - (input.key(minus) ? 1f : 0f); }

    @Override public void input() {
        window.poll();
        if (window.consumeResize()) {
            glViewport(0, 0, window.width(), window.height());
            camera.setAspect(window.width(), window.height());
        }
        if (pending == null && fade < .5f) {
            switch (state) {
                case MAIN_MENU -> mainMenuInput();
                case NAME_ENTRY -> nameInput();
                case HERO_SELECT -> heroSelectInput();
                case CHAPTER_SELECT -> chapterSelectInput();
                case CONTROLS, SCORES -> { if (input.pressed(GLFW_KEY_ESCAPE) || confirm()) { audio.play(Sound.UI_SELECT); state = State.MAIN_MENU; } }
                case DIALOGUE -> dialogueInput();
                case PLAYING -> playingInput();
                case PAUSED -> pausedInput();
                case LEVEL_COMPLETE -> {
                    if (confirm()) {
                        audio.play(Sound.UI_SELECT);
                        Level lvl = world.level;
                        startDialogue(lvl.outro, () -> transition(() -> { if (chapter < 4) beginChapter(chapter + 1); else toEnding(); }));
                    }
                }
                case GAME_OVER -> gameOverInput();
                case ENDING -> { if (confirm() || input.pressed(GLFW_KEY_ESCAPE)) transition(this::goMainMenu); }
            }
        }
        input.endFrame();
    }

    private void mainMenuInput() {
        int d = navigate(MAIN_ITEMS.length);
        if (d != 0) { menuIndex = (menuIndex + d + MAIN_ITEMS.length) % MAIN_ITEMS.length; audio.play(Sound.UI_MOVE); }
        if (input.pressed(GLFW_KEY_ESCAPE)) { window.requestClose(); return; }
        if (!confirm()) return;
        audio.play(Sound.UI_SELECT);
        switch (menuIndex) {
            case 0 -> { storyMode = true; if (save.unlocked > 1) { chapterSel = save.unlocked; state = State.CHAPTER_SELECT; } else { chapter = 1; state = State.HERO_SELECT; } }
            case 1 -> { storyMode = false; state = State.HERO_SELECT; }
            case 2 -> state = State.SCORES;
            case 3 -> state = State.CONTROLS;
            case 4 -> { nameBuffer = save.playerName; state = State.NAME_ENTRY; }
            default -> window.requestClose();
        }
    }

    private void nameInput() {
        String typed = input.text();
        if (!typed.isEmpty() && nameBuffer.length() < 12) nameBuffer = (nameBuffer + typed).toUpperCase();
        if (nameBuffer.length() > 12) nameBuffer = nameBuffer.substring(0, 12);
        if (input.pressed(GLFW_KEY_BACKSPACE) && !nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
        if (input.pressed(GLFW_KEY_ENTER)) {
            save.playerName = nameBuffer.trim().isEmpty() ? "PLAYER" : nameBuffer.trim();
            save.save();
            audio.play(Sound.UI_SELECT);
            state = State.MAIN_MENU;
        } else if (input.pressed(GLFW_KEY_ESCAPE) && !save.playerName.isEmpty()) state = State.MAIN_MENU;
    }

    private void selectHero(int idx) {
        heroIndex = (idx + Heroes.ALL.length) % Heroes.ALL.length;
        HeroDef d = Heroes.ALL[heroIndex];
        world.showcaseHero(d);
        world.player.playAnim(d.attackAnim, d.attackTime + .2f);          // little demo of the attack animation
        audio.play(Sound.UI_MOVE);
    }

    private void heroSelectInput() {
        if (input.pressed(GLFW_KEY_RIGHT) || input.pressed(GLFW_KEY_D)) selectHero(heroIndex + 1);
        if (input.pressed(GLFW_KEY_LEFT) || input.pressed(GLFW_KEY_A)) selectHero(heroIndex - 1);
        if (input.pressed(GLFW_KEY_UP) || input.pressed(GLFW_KEY_W)) { HeroDef d = Heroes.ALL[heroIndex]; world.player.playAnim(d.specialAnim, d.specialTime + .2f); audio.play(Sound.UI_MOVE); }
        if (input.pressed(GLFW_KEY_ESCAPE)) { state = storyMode && save.unlocked > 1 ? State.CHAPTER_SELECT : State.MAIN_MENU; return; }
        if (input.pressed(GLFW_KEY_ENTER) || input.pressed(GLFW_KEY_SPACE)) {
            currentHero = Heroes.ALL[heroIndex];
            audio.play(Sound.UI_SELECT);
            if (storyMode) transition(() -> beginChapter(chapter));
            else transition(this::beginArena);
        }
    }

    private void chapterSelectInput() {
        int d = navigate(save.unlocked);
        if (d != 0) { chapterSel = Math.max(1, Math.min(save.unlocked, chapterSel + d)); audio.play(Sound.UI_MOVE); }
        if (input.pressed(GLFW_KEY_ESCAPE)) { state = State.MAIN_MENU; return; }
        if (confirm()) { chapter = chapterSel; audio.play(Sound.UI_SELECT); state = State.HERO_SELECT; }
    }

    private void dialogueInput() {
        if (input.pressed(GLFW_KEY_ESCAPE)) { Runnable r = onDialogueDone; onDialogueDone = null; if (r != null) r.run(); return; }
        if (!(confirm() || input.mousePressed(GLFW_MOUSE_BUTTON_LEFT))) return;
        String full = speaker(lines[lineIdx].text());
        if ((int) (typeT * 48f) < full.length()) { typeT = 999f; return; }
        audio.play(Sound.UI_SELECT);
        lineIdx++; typeT = 0f;
        if (lineIdx >= lines.length) { Runnable r = onDialogueDone; onDialogueDone = null; if (r != null) r.run(); }
    }

    private void pausedInput() {
        int d = navigate(3);
        if (d != 0) { menuIndex = (menuIndex + d + 3) % 3; audio.play(Sound.UI_MOVE); }
        if (input.pressed(GLFW_KEY_ESCAPE)) { state = State.PLAYING; capture(true); return; }
        if (!confirm()) return;
        audio.play(Sound.UI_SELECT);
        switch (menuIndex) {
            case 0 -> { state = State.PLAYING; capture(true); }
            case 1 -> transition(this::retry);
            default -> transition(this::goMainMenu);
        }
    }

    private void gameOverInput() {
        int d = navigate(2);
        if (d != 0) { menuIndex = (menuIndex + d + 2) % 2; audio.play(Sound.UI_MOVE); }
        if (!confirm()) return;
        audio.play(Sound.UI_SELECT);
        if (menuIndex == 0) transition(this::retry); else transition(this::goMainMenu);
    }

    private void playingInput() {
        if (input.pressed(GLFW_KEY_ESCAPE)) { state = State.PAUSED; menuIndex = 0; capture(false); return; }
        Hero p = world.player;
        camera.rotate((float) input.mouseDX(), (float) input.mouseDY(), .0025f);
        camera.zoom((float) input.scroll());
        if (input.pressed(GLFW_KEY_V)) camera.toggleMode();

        camera.flatForward(fwd);
        camera.flatRight(rgt);
        move.set(0, 0, 0).fma(axis(GLFW_KEY_W, GLFW_KEY_S), fwd).fma(axis(GLFW_KEY_D, GLFW_KEY_A), rgt);
        if (move.lengthSquared() > 1f) move.normalize();
        p.moveInput.set(move);
        if (p.dead) return;

        Vector3f pos = p.transform().position();
        if (input.pressed(GLFW_KEY_SPACE) && p.jump()) audio.play(Sound.JUMP, pos);
        if (input.pressed(GLFW_KEY_F) && p.toggleFlight()) audio.play(Sound.JUMP, pos);
        if (p.flying) p.setVerticalInput(axis(GLFW_KEY_SPACE, GLFW_KEY_LEFT_SHIFT));
        if (input.pressed(GLFW_KEY_LEFT_CONTROL)) p.groundPound();
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) && !p.flying) {
            Vector3f dir = move.lengthSquared() > .01f ? move : fwd;
            if (p.tryDodge(dir)) audio.play(Sound.DODGE, pos);
        }
        if (input.pressed(GLFW_KEY_Q)) p.trySpecial(world, camera);
        if (input.mouseDown(GLFW_MOUSE_BUTTON_LEFT) || input.key(GLFW_KEY_J)) p.tryAttack(world, camera);
    }

    // ======================= simulation =======================
    private void menuCamera() {
        float sc = world.player != null ? world.player.def.look.scale : 1f;
        camera.configure(1.2f * sc, 1.05f * sc, 4.6f + 3.4f * sc);
        camera.setAngles((float) Math.sin(time * .5f) * .75f, -.08f);
    }

    @Override public void update(float dt) {
        time += dt;
        if (fadeDir > 0) {
            fade = Math.min(1f, fade + dt * 3.2f);
            if (fade >= 1f) { fadeDir = -1; Runnable r = pending; pending = null; if (r != null) r.run(); }
        } else if (fadeDir < 0) {
            fade = Math.max(0f, fade - dt * 2.4f);
            if (fade <= 0f) fadeDir = 0;
        }

        float sdt = dt;
        if (world.pendingHitStop > 0f) { hitStopT = Math.max(hitStopT, world.pendingHitStop); world.pendingHitStop = 0f; }
        if (hitStopT > 0f) { hitStopT -= dt; sdt = dt * .06f; }
        if (world.pendingShake > 0f) { camera.addShake(world.pendingShake); world.pendingShake = 0f; }

        switch (state) {
            case MAIN_MENU, NAME_ENTRY, HERO_SELECT, CHAPTER_SELECT, CONTROLS, SCORES -> { menuCamera(); world.updateShowcase(dt); }
            case ENDING -> { menuCamera(); creditsT += dt; world.updateShowcase(dt); }
            case DIALOGUE -> {
                typeT += dt;
                camera.setAngles((float) Math.sin(time * .4f) * .9f + .3f, -.16f);
                world.updateShowcase(dt);
            }
            case PLAYING -> {
                world.update(sdt);
                Hero p = world.player;
                float spd = (float) Math.sqrt(p.velocity.x * p.velocity.x + p.velocity.z * p.velocity.z);
                camera.setFovBoost(p.flying ? Math.min(.2f, spd / 17f * .2f) : (p.dashTimer > 0f ? .14f : 0f));
                if (p.dead && world.deathTimer > 2f) {
                    lastRank = storyMode ? -1 : save.submitScore(save.playerName, world.score);
                    audio.play(Sound.GAMEOVER);
                    capture(false);
                    state = State.GAME_OVER; menuIndex = 0;
                } else if (world.levelComplete && world.completeTimer > 2.5f) {
                    completeGradeHp = p.health / p.maxHealth;
                    grade = completeGradeHp > .75f && world.elapsed < 300f ? "S" : completeGradeHp > .5f ? "A" : completeGradeHp > .25f ? "B" : "C";
                    if (chapter >= 4) save.completed = true;
                    save.unlock(chapter + 1);
                    save.save();
                    capture(false);
                    state = State.LEVEL_COMPLETE;
                }
            }
            default -> { }
        }
        if (state != State.PAUSED) {
            particles.update(sdt);
            world.fx.ambient(world.level.env.ambient, camera, dt);
        }
    }

    // ======================= rendering =======================
    @Override public void render(float alpha) {
        long now = System.nanoTime();
        float fdt = Math.min(.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;

        Environment env = world.level.env;
        Vector3f focus = world.player != null ? world.player.transform().position() : ORIGIN;
        camera.follow(focus);
        camera.update(fdt);
        audio.setListener(camera.eye(), camera.forward(), camera.up());

        List<GameObject3D> objs = world.drawables();
        renderer.prepare(objs);
        Lights lights = world.fillLights();
        shadow.update(focus, env.sunDir, 42f);
        renderer.shadowPass(objs, shadow, focus);

        post.begin(window.width(), window.height());
        renderer.scene(camera, objs, env, lights, shadow, time);
        particles.draw(camera);
        postParams.exposure = env.exposure; postParams.bloom = env.bloom; postParams.vignette = env.vignette;
        postParams.saturation = env.saturation; postParams.fade = fade;
        postParams.overlayR = postParams.overlayG = postParams.overlayB = postParams.overlayA = 0f;
        if (world.damageFlash > 0f) { postParams.overlayR = 1f; postParams.overlayG = .08f; postParams.overlayB = .08f; postParams.overlayA = .35f * world.damageFlash; }
        else if (world.healFlash > 0f) { postParams.overlayR = .3f; postParams.overlayG = 1f; postParams.overlayB = .4f; postParams.overlayA = .12f * world.healFlash; }
        else if (state == State.PLAYING && world.player != null && world.player.health < world.player.maxHealth * .3f && !world.player.dead) {
            postParams.overlayR = 1f; postParams.overlayG = 0f; postParams.overlayB = 0f; postParams.overlayA = .07f + .05f * (float) Math.sin(time * 6f);
        }
        if (world.player != null && world.player.dead) postParams.saturation *= Math.max(.2f, 1f - world.player.deathT * .5f);
        post.end(postParams);

        drawUi();
        window.swap();
    }

    // ======================= UI screens =======================
    private void center(String t, float y, float scale, float r, float g, float b, float w) {
        ui.text(t, (w - ui.textWidth(t, scale)) / 2f, y, scale, r, g, b);
    }

    private void menuList(String[] items, int sel, float x, float y, float step, float scale, float s) {
        for (int i = 0; i < items.length; i++) {
            boolean on = i == sel;
            if (on) ui.rect(x - 16 * s, y + i * step - 4 * s, 340 * s, step - 2 * s, 1f, .3f, .25f, .25f);
            ui.text((on ? "> " : "  ") + items[i], x, y + i * step, scale, on ? 1f : .75f, on ? .85f : .75f, on ? .3f : .8f);
        }
    }

    private void overlay(float w, float h, float r, float g, float b, float a) { ui.rect(0, 0, w, h, r, g, b, a); }

    private List<String> wrap(String text, float maxW, float scale) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (ui.textWidth(test, scale) > maxW && cur.length() > 0) { out.add(cur.toString()); cur = new StringBuilder(word); }
            else { cur.setLength(0); cur.append(test); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private void drawUi() {
        float w = window.width(), h = window.height(), s = h / 720f;
        ui.begin(w, h);
        switch (state) {
            case MAIN_MENU -> drawMain(w, h, s);
            case NAME_ENTRY -> drawName(w, h, s);
            case HERO_SELECT -> drawHeroSelect(w, h, s);
            case CHAPTER_SELECT -> drawChapterSelect(w, h, s);
            case CONTROLS -> drawControls(w, h, s);
            case SCORES -> drawScores(w, h, s);
            case DIALOGUE -> drawDialogue(w, h, s);
            case PLAYING -> hud.draw(world, camera, w, h, s);
            case PAUSED -> {
                hud.draw(world, camera, w, h, s);
                overlay(w, h, 0, 0, .03f, .6f);
                center("PAUSED", h * .22f, 8 * s, 1, 1, 1, w);
                menuList(new String[]{"RESUME", storyMode ? "RESTART CHAPTER" : "RESTART ARENA", "QUIT TO MENU"}, menuIndex, w / 2f - 130 * s, h * .42f, 44 * s, 3.2f * s, s);
            }
            case LEVEL_COMPLETE -> drawComplete(w, h, s);
            case GAME_OVER -> {
                hud.draw(world, camera, w, h, s);
                overlay(w, h, .25f, 0, 0, .65f);
                center("YOU HAVE FALLEN", h * .16f, 7 * s, 1, .25f, .25f, w);
                center("SCORE " + world.score + "     KILLS " + world.kills + "     WAVE " + world.wave, h * .34f, 3f * s, 1, 1, 1, w);
                if (lastRank >= 0) center("NEW HIGH SCORE!  RANK #" + (lastRank + 1), h * .42f, 3.4f * s, 1f, .85f, .2f, w);
                menuList(new String[]{storyMode ? "RETRY CHAPTER" : "TRY AGAIN", "QUIT TO MENU"}, menuIndex, w / 2f - 120 * s, h * .56f, 46 * s, 3.4f * s, s);
            }
            case ENDING -> drawEnding(w, h, s);
        }
        ui.end();
    }

    private void drawMain(float w, float h, float s) {
        center("MARVEL", h * .08f, 5f * s, 1f, .3f, .3f, w);
        center("AVENGERS 3D", h * .15f, 9f * s, 1f, .25f, .2f, w);
        center("ASSEMBLE. FIGHT. SURVIVE.", h * .30f, 2.4f * s, .8f, .85f, 1f, w);
        menuList(MAIN_ITEMS, menuIndex, 70 * s, h * .42f, 46 * s, 3.2f * s, s);
        ui.text("PLAYER: " + save.playerName, 24 * s, h - 40 * s, 2.2f * s, .8f, .85f, 1f);
        ui.text("W/S OR UP/DOWN: MOVE     ENTER: SELECT", w - 480 * s, h - 40 * s, 1.9f * s, .7f, .7f, .8f);
        if (save.unlocked > 1) ui.text("STORY PROGRESS: CHAPTER " + save.unlocked + " / 4" + (save.completed ? "  (COMPLETED)" : ""), 24 * s, h - 70 * s, 1.9f * s, 1f, .85f, .3f);
    }

    private void drawName(float w, float h, float s) {
        overlay(w, h, 0, 0, .04f, .55f);
        center("WHAT SHOULD WE CALL YOU?", h * .28f, 4f * s, 1f, 1f, 1f, w);
        String shown = nameBuffer + (((int) (time * 2f) & 1) == 0 ? "_" : " ");
        ui.rect(w / 2f - 260 * s, h * .42f - 12 * s, 520 * s, 60 * s, 0, 0, 0, .7f);
        center(shown, h * .42f, 5f * s, 1f, .85f, .3f, w);
        center("TYPE YOUR NAME (UP TO 12 CHARACTERS)  -  ENTER TO CONFIRM", h * .62f, 2f * s, .75f, .8f, .95f, w);
    }

    private void drawHeroSelect(float w, float h, float s) {
        HeroDef d = Heroes.ALL[heroIndex];
        float[] th = d.theme;
        ui.rect(0, 0, w * .46f, h, 0, 0, .03f, .55f);
        ui.text(storyMode ? "STORY MODE  -  CHOOSE YOUR HERO" : "ARENA  -  CHOOSE YOUR HERO", 40 * s, 28 * s, 2.2f * s, .8f, .85f, 1f);
        ui.text(d.name, 40 * s, 66 * s, 6.5f * s, th[0], th[1], th[2]);
        ui.text(d.tagline, 40 * s, 140 * s, 2.4f * s, 1f, 1f, 1f);
        float y = 190 * s;
        ui.text("HEALTH", 40 * s, y, 2f * s, .8f, .8f, .9f);   bar(210 * s, y, 260 * s, 14 * s, d.health / 180f, th);
        ui.text("SPEED", 40 * s, y + 28 * s, 2f * s, .8f, .8f, .9f); bar(210 * s, y + 28 * s, 260 * s, 14 * s, d.walkSpeed / 9f, th);
        ui.text("FIRE RATE", 40 * s, y + 56 * s, 2f * s, .8f, .8f, .9f); bar(210 * s, y + 56 * s, 260 * s, 14 * s, Math.min(1f, .25f / d.attackCd), th);
        y += 100 * s;
        ui.text("LMB   " + d.attackName, 40 * s, y, 2.6f * s, th[0], th[1], th[2]);
        ui.text("Q     " + d.specialName + "  (" + (int) d.specialCd + " s)", 40 * s, y + 32 * s, 2.6f * s, th[0], th[1], th[2]);
        for (int i = 0; i < d.perks.length; i++) ui.text("- " + d.perks[i], 40 * s, y + 80 * s + i * 26 * s, 1.9f * s, .85f, .85f, .95f);
        float rowY = h - 96 * s;
        for (int i = 0; i < Heroes.ALL.length; i++) {
            boolean on = i == heroIndex;
            float bx = 40 * s + i * 210 * s;
            ui.rect(bx - 4 * s, rowY - 6 * s, 196 * s, 34 * s, on ? Heroes.ALL[i].theme[0] : .1f, on ? Heroes.ALL[i].theme[1] : .1f, on ? Heroes.ALL[i].theme[2] : .14f, on ? .6f : .5f);
            ui.text(Heroes.ALL[i].name, bx, rowY, 1.9f * s, on ? 1f : .7f, on ? 1f : .7f, on ? 1f : .8f);
        }
        ui.text("A/D: CHANGE HERO     W: PREVIEW SPECIAL     ENTER: SELECT     ESC: BACK", 40 * s, h - 40 * s, 1.9f * s, .75f, .8f, .9f);
    }

    private void bar(float x, float y, float bw, float bh, float frac, float[] th) {
        ui.rect(x - 2, y - 2, bw + 4, bh + 4, 0, 0, 0, .6f);
        ui.rect(x, y, bw * Math.max(0f, Math.min(1f, frac)), bh, th[0], th[1], th[2], 1f);
    }

    private void drawChapterSelect(float w, float h, float s) {
        overlay(w, h, 0, 0, .04f, .5f);
        center("SELECT CHAPTER", h * .12f, 5.5f * s, 1f, .85f, .3f, w);
        for (int i = 1; i <= 4; i++) {
            boolean unlocked = i <= save.unlocked, on = i == chapterSel;
            float y = h * .30f + (i - 1) * 74 * s;
            ui.rect(w / 2f - 330 * s, y - 8 * s, 660 * s, 62 * s, on ? 1f : .1f, on ? .3f : .1f, on ? .25f : .14f, on ? .35f : .5f);
            if (unlocked) {
                ui.text("CHAPTER " + i + ":  " + CH_TITLE[i - 1], w / 2f - 310 * s, y, 3.2f * s, on ? 1f : .8f, on ? .9f : .8f, on ? .3f : .9f);
                ui.text(CH_PLACE[i - 1], w / 2f - 310 * s, y + 30 * s, 1.9f * s, .7f, .75f, .9f);
            } else ui.text("CHAPTER " + i + ":  LOCKED", w / 2f - 310 * s, y + 8 * s, 3f * s, .4f, .4f, .45f);
        }
        center("W/S: CHOOSE     ENTER: CONTINUE     ESC: BACK", h * .92f, 2f * s, .75f, .8f, .95f, w);
    }

    private void drawControls(float w, float h, float s) {
        overlay(w, h, 0, 0, .04f, .7f);
        center("CONTROLS", h * .08f, 6f * s, 1f, .85f, .3f, w);
        String[] rows = {
            "W A S D ............ MOVE", "MOUSE .............. LOOK / AIM", "SPACE .............. JUMP (HOLD TO ASCEND WHEN FLYING)",
            "LEFT MOUSE / J ..... ATTACK (HOLD TO KEEP FIRING)", "Q ................... SPECIAL ABILITY", "LEFT SHIFT ......... DODGE ROLL (DESCEND WHEN FLYING)",
            "F ................... TOGGLE FLIGHT (IRON MAN)", "LEFT CTRL .......... GROUND POUND IN MID-AIR (HULK)",
            "V ................... FIRST / THIRD PERSON", "MOUSE WHEEL ........ CAMERA ZOOM", "ESC ................. PAUSE"};
        for (int i = 0; i < rows.length; i++) ui.text(rows[i], w / 2f - 380 * s, h * .22f + i * 38 * s, 2.5f * s, .9f, .92f, 1f);
        center("ENTER OR ESC: BACK", h * .92f, 2.2f * s, .75f, .8f, .95f, w);
    }

    private void drawScores(float w, float h, float s) {
        overlay(w, h, 0, 0, .04f, .7f);
        center("HIGH SCORES  -  ARENA", h * .12f, 6f * s, 1f, .85f, .3f, w);
        for (int i = 0; i < 5; i++) {
            center((i + 1) + ".   " + save.topName[i] + "   " + save.topScore[i], h * .32f + i * 58 * s, 3.8f * s, i == 0 ? 1f : .85f, i == 0 ? .85f : .88f, i == 0 ? .3f : 1f, w);
        }
        center("ENTER OR ESC: BACK", h * .92f, 2.2f * s, .75f, .8f, .95f, w);
    }

    private void drawDialogue(float w, float h, float s) {
        float bar = h * .1f;
        ui.rect(0, 0, w, bar, 0, 0, 0, 1f);
        ui.rect(0, h - bar, w, bar, 0, 0, 0, 1f);
        if (world.level.id > 0) {
            ui.text("CHAPTER " + world.level.id + "  -  " + world.level.title, 40 * s, 24 * s, 3f * s, 1f, .85f, .3f);
            ui.text(world.level.location, 40 * s, 24 * s + 30 * s, 1.9f * s, .8f, .85f, 1f);
        }
        if (lineIdx >= lines.length) return;
        Level.Line ln = lines[lineIdx];
        String full = speaker(ln.text());
        int shown = Math.min(full.length(), (int) (typeT * 48f));
        float panelH = 150 * s, py = h - bar - panelH - 20 * s;
        ui.rect(w * .1f, py, w * .8f, panelH, 0, 0, .03f, .8f);
        ui.rect(w * .1f, py, 6 * s, panelH, 1f, .3f, .25f, 1f);
        boolean hero = ln.speaker().equals("{HERO}");
        float[] th = currentHero.theme;
        ui.text(speaker(ln.speaker()), w * .1f + 24 * s, py + 14 * s, 3f * s, hero ? th[0] : 1f, hero ? th[1] : .85f, hero ? th[2] : .4f);
        List<String> wrapped = wrap(full.substring(0, shown), w * .8f - 60 * s, 2.5f * s);
        for (int i = 0; i < wrapped.size() && i < 4; i++) ui.text(wrapped.get(i), w * .1f + 24 * s, py + 52 * s + i * 26 * s, 2.5f * s, 1f, 1f, 1f);
        if (shown >= full.length()) ui.text("ENTER >", w * .9f - 130 * s, py + panelH - 30 * s, 2f * s, .8f, .85f, 1f);
        ui.text("ESC: SKIP", w - 150 * s, bar * .35f, 1.8f * s, .6f, .65f, .75f);
    }

    private void drawComplete(float w, float h, float s) {
        overlay(w, h, 0, .04f, 0, .6f);
        center("CHAPTER COMPLETE", h * .12f, 7.5f * s, .4f, 1f, .5f, w);
        center(world.level.title, h * .26f, 3.6f * s, 1f, .85f, .3f, w);
        center("RANK", h * .36f, 2.5f * s, .8f, .85f, 1f, w);
        center(grade, h * .40f, 14f * s, grade.equals("S") ? 1f : .9f, grade.equals("S") ? .85f : .9f, grade.equals("S") ? .2f : 1f, w);
        center("SCORE " + world.score + "     KILLS " + world.kills + "     TIME " + (int) world.elapsed + " s", h * .68f, 3f * s, 1, 1, 1, w);
        center("HEALTH LEFT " + (int) (completeGradeHp * 100) + "%", h * .75f, 2.6f * s, .85f, .9f, 1f, w);
        center("PRESS ENTER", h * .88f, 2.6f * s, .8f, .85f, 1f, w);
    }

    private void drawEnding(float w, float h, float s) {
        overlay(w, h, 0, 0, .03f, .35f);
        String[] credits = {"MARVEL AVENGERS 3D", "", "YOU SAVED THE CITY.", "", save.playerName + " AS " + currentHero.name, "", "ENGINE: JAVA 17  -  LWJGL 3  -  OPENGL 3.3",
            "HDR BLOOM  -  SHADOW MAPS  -  PROCEDURAL SKELETAL ANIMATION", "PROCEDURAL AUDIO  -  PROCEDURAL TEXTURES", "", "THANKS FOR PLAYING", "", "PRESS ENTER"};
        float y = h - creditsT * 55f * s;
        for (int i = 0; i < credits.length; i++) {
            float yy = y + i * 54 * s;
            if (yy > -60 * s && yy < h) center(credits[i], yy, i == 0 ? 7f * s : 2.8f * s, 1f, i == 0 ? .3f : .9f, i == 0 ? .3f : 1f, w);
        }
        if (y + credits.length * 54 * s < h * .5f) center("PRESS ENTER", h * .5f, 3f * s, .9f, .9f, 1f, w);
    }
}
