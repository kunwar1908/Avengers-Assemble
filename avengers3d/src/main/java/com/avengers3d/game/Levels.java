package com.avengers3d.game;

import com.avengers3d.render.Environment;

import java.util.Random;

import static com.avengers3d.game.Level.Line;
import static com.avengers3d.game.Level.WaveSpec;

/** The four story chapters, the endless arena and the hero-select stage. Geometry is plain code: tweak freely. */
public final class Levels {
    private Levels() { }

    private static Line l(String who, String text) { return new Line(who, text); }

    // ======================= CHAPTER 1: DOWNTOWN =======================
    public static Level chapter1(Assets a) {
        Level L = new Level(1, "STREET WAR", "DOWNTOWN  -  18:42", "DEFEAT 3 WAVES OF UMBRA TROOPS");
        Environment e = L.env;
        e.skyTop = new float[]{.10f, .13f, .34f}; e.skyHorizon = new float[]{1f, .55f, .28f};
        e.sunDir = new float[]{-.55f, -.32f, -.5f}; e.sunColor = new float[]{3.2f, 1.9f, 1.0f};
        e.ambientSky = new float[]{.22f, .26f, .42f}; e.ambientGround = new float[]{.12f, .09f, .09f};
        e.fog = new float[]{.80f, .50f, .38f}; e.fogDensity = .011f; e.clouds = .9f; e.stars = .2f;
        e.exposure = 1.05f; e.bloom = .85f; e.ambient = Environment.Ambient.DUST;
        L.half = 32f; L.groundTex = a.asphalt; L.groundScale = 7f; L.music = 1;
        L.ringSpawns(25f, 16);
        L.waves = new WaveSpec[]{new WaveSpec(4, 0, 0, 0), new WaveSpec(5, 2, 0, 0), new WaveSpec(5, 3, 1, 0)};
        L.intro = new Line[]{
            l("COMMAND", "All teams, this is Command. The Umbra Syndicate has locked down the city centre with armed troopers."),
            l("{HERO}", "I'm on it. Where is their command post?"),
            l("COMMAND", "Unknown. Clear the streets and we'll trace their signal. Watch out for their drones.")};
        L.outro = new Line[]{
            l("COMMAND", "Signal traced. It leads to an old foundry on the east side. They're building something big."),
            l("{HERO}", "Then let's shut it down.")};
        L.builder = w -> {
            Random rnd = new Random(11);
            for (int side = 0; side < 4; side++)
                for (int i = 0; i < 6; i++) {
                    float along = -35 + i * 14, h = 14 + rnd.nextFloat() * 18, shade = .55f + rnd.nextFloat() * .35f;
                    float x = side < 2 ? along : (side == 2 ? -41f : 41f), z = side == 0 ? -41f : (side == 1 ? 41f : along);
                    w.solid(a.cube, x, 0, z, 14, h, 14, shade * .9f, shade * .92f, shade).tex(a.windows, 7f).spec(.3f);
                    w.prop(a.cube, x, h, z, 1, .6f, 1, 1f, .1f, .1f).emit(3f).noShadow();
                }
            for (int i = 0; i < 36; i++) {                            // distant skyline
                double ang = rnd.nextDouble() * Math.PI * 2; float rad = 75 + rnd.nextFloat() * 60, h = 30 + rnd.nextFloat() * 60, s = 10 + rnd.nextFloat() * 14;
                w.prop(a.cube, (float) Math.cos(ang) * rad, 0, (float) Math.sin(ang) * rad, s, h, s, .5f, .5f, .6f).tex(a.windows, 8f).noShadow();
            }
            w.prop(a.cube, 0, 0, -130, 22, 160, 22, .6f, .65f, .8f).tex(a.windows, 9f).noShadow();      // landmark tower
            w.prop(a.cube, 0, 160, -130, 24, 2, 24, 1f, 1f, 1f).emit(2f).noShadow();
            float[][] cars = {{-10, 10, 0}, {12, -6, 1}, {-18, -14, 0}, {18, 16, 1}, {4, 21, 0}, {-22, 6, 1}};
            float[][] pal = {{.7f, .1f, .1f}, {.1f, .25f, .6f}, {.75f, .75f, .78f}, {.15f, .15f, .17f}, {.8f, .6f, .1f}, {.1f, .5f, .3f}};
            for (int i = 0; i < cars.length; i++) {
                boolean rot = cars[i][2] > 0; float sx = rot ? 2.1f : 4.4f, sz = rot ? 4.4f : 2.1f;
                float[] c = pal[i];
                w.solid(a.cube, cars[i][0], 0, cars[i][1], sx, 1.2f, sz, c[0], c[1], c[2]).spec(.9f);
                w.prop(a.cube, cars[i][0], 1.2f, cars[i][1], sx * (rot ? 1f : .55f), .5f, sz * (rot ? .55f : 1f), .08f, .1f, .14f).spec(.9f);
                w.prop(a.cube, cars[i][0] + (rot ? 0 : sx / 2), .4f, cars[i][1] + (rot ? sz / 2 : 0), rot ? 1.6f : .1f, .22f, rot ? .1f : 1.6f, 1f, .1f, .1f).emit(3f).noShadow();
            }
            w.solid(a.cube, 12, 0, 10, 10, 3, 2.6f, .8f, .65f, .1f).spec(.5f);                          // bus
            for (int i = 0; i < 5; i++) w.solid(a.cube, -6 + i * 3.2f, 0, -22, 3f, 1.1f, .9f, .6f, .6f, .62f).tex(a.concrete, 3f);   // jersey barriers
            w.solid(a.cube, -22, 0, -4, 3, 2.2f, 3, .5f, .35f, .2f).tex(a.plate, 3f);                  // crates
            w.solid(a.cube, -22, 2.2f, -4, 3, 2.2f, 3, .5f, .35f, .2f).tex(a.plate, 3f);
            w.solid(a.cube, 22, 0, -4, 6, 5, 6, .6f, .6f, .65f).tex(a.concrete, 4f);                    // kiosk
            for (int i = 0; i < 8; i++) {                                                               // street lamps
                double ang = i * Math.PI / 4 + .4; float lx = (float) Math.cos(ang) * 27f, lz = (float) Math.sin(ang) * 27f;
                w.prop(a.cylinder, lx, 0, lz, .3f, 7f, .3f, .2f, .2f, .22f);
                w.prop(a.cube, lx, 7f, lz, 1f, .25f, 1f, 1f, .8f, .5f).emit(4f).noShadow();
            }
        };
        return L;
    }

    // ======================= CHAPTER 2: THE FOUNDRY =======================
    public static Level chapter2(Assets a) {
        Level L = new Level(2, "THE FOUNDRY", "EAST INDUSTRIAL  -  23:10", "DEFEAT COMMANDER VEX");
        Environment e = L.env;
        e.skyTop = new float[]{.04f, .02f, .03f}; e.skyHorizon = new float[]{.55f, .16f, .05f};
        e.sunDir = new float[]{-.2f, -.9f, -.3f}; e.sunColor = new float[]{1.0f, .6f, .45f};
        e.ambientSky = new float[]{.18f, .10f, .10f}; e.ambientGround = new float[]{.09f, .05f, .04f};
        e.fog = new float[]{.28f, .11f, .06f}; e.fogDensity = .017f; e.clouds = .5f; e.stars = 0f;
        e.exposure = 1.15f; e.bloom = 1.1f; e.saturation = 1.1f; e.ambient = Environment.Ambient.EMBERS;
        L.half = 30f; L.groundTex = a.grate; L.groundScale = 4f; L.music = 2; L.bossZ = -14f;
        L.ringSpawns(24f, 14);
        L.waves = new WaveSpec[]{new WaveSpec(5, 2, 0, 0), new WaveSpec(4, 4, 1, 0), new WaveSpec(2, 0, 0, 1)};
        L.intro = new Line[]{
            l("COMMAND", "Reactor output is off the charts. Whatever they're charging, it's close to done."),
            l("VEX", "You're too late, defender. Warden Omega rises tonight - and you'll be the first to kneel."),
            l("{HERO}", "Big talk. Let's see you back it up.")};
        L.outro = new Line[]{
            l("VEX", "Impossible... Omega... will not... be stopped."),
            l("COMMAND", "Vex was carrying launch coordinates - a mountain ridge to the north. That's their fuel depot."),
            l("{HERO}", "Cold weather it is.")};
        L.builder = w -> {
            float[] steel = {.55f, .56f, .62f};
            w.solid(a.cube, 0, 0, -33, 70, 16, 2, steel[0], steel[1], steel[2]).tex(a.plate, 4f).spec(.5f);
            w.solid(a.cube, 0, 0, 33, 70, 16, 2, steel[0], steel[1], steel[2]).tex(a.plate, 4f).spec(.5f);
            w.solid(a.cube, -33, 0, 0, 2, 16, 70, steel[0], steel[1], steel[2]).tex(a.plate, 4f).spec(.5f);
            w.solid(a.cube, 33, 0, 0, 2, 16, 70, steel[0], steel[1], steel[2]).tex(a.plate, 4f).spec(.5f);
            for (int i = 0; i < 14; i++) {                                                  // vertical pipes with hot glowing bands
                float px = -30 + i * 4.6f;
                w.prop(a.cylinder, px, 0, -31.6f, .9f, 14, .9f, .4f, .35f, .33f).spec(.7f);
                w.prop(a.cylinder, px, 4f, -31.6f, 1f, .4f, 1f, 1f, .45f, .1f).emit(3.5f).noShadow();
                w.prop(a.cylinder, px, 9f, -31.6f, 1f, .4f, 1f, 1f, .45f, .1f).emit(3.5f).noShadow();
            }
            w.solid(a.cylinder, 0, 0, -14, 10, 9, 10, .25f, .26f, .3f).tex(a.plate, 4f).spec(.6f);            // reactor
            w.prop(a.sphere, 0, 9f, -14, 6.5f, 6.5f, 6.5f, 1f, .45f, .1f).emit(3.2f).noShadow();
            w.prop(a.cylinder, 0, 4.5f, -14, 10.6f, .5f, 10.6f, 1f, .4f, .1f).emit(3f).noShadow();
            for (int s = -1; s <= 1; s += 2) {                                              // staircases up to catwalks
                w.solid(a.cube, s * 18f, 0, 8, 5, 1.1f, 6, .4f, .4f, .44f).tex(a.grate, 3f);
                w.solid(a.cube, s * 23f, 0, 8, 5, 2.2f, 6, .4f, .4f, .44f).tex(a.grate, 3f);
                w.solid(a.cube, s * 12f, 0, 20, 2.4f, 12, 2.4f, .3f, .3f, .34f).tex(a.plate, 3f);
                w.prop(a.cylinder, s * 12f, 6f, 20, 3.2f, .5f, 3.2f, 1f, .5f, .15f).emit(3f).noShadow();
                w.prop(a.cylinder, s * 12f, 9f, 20, 3.2f, .5f, 3.2f, 1f, .5f, .15f).emit(3f).noShadow();
            }
            for (int i = 0; i < 7; i++) {                                                   // crates / cover
                float cx = -16 + i * 5.2f, cz = i % 2 == 0 ? -4 : 2;
                w.solid(a.cube, cx, 0, cz, 2.2f, 2f, 2.2f, .6f, .45f, .25f).tex(a.plate, 2f);
            }
            for (int i = 0; i < 5; i++) w.prop(a.cube, -28 + i * 14f, 11f, 32f, 4f, .3f, .3f, 1f, .5f, .15f).emit(4f).noShadow();
            for (int i = 0; i < 6; i++) {                                                   // chimneys outside
                float cx = -60 + i * 24f;
                w.prop(a.cylinder, cx, 0, -70, 8, 50 + i * 4, 8, .22f, .2f, .22f).noShadow();
                w.prop(a.cylinder, cx, 50 + i * 4, -70, 8.4f, .8f, 8.4f, 1f, .4f, .1f).emit(3f).noShadow();
            }
        };
        return L;
    }

    // ======================= CHAPTER 3: FROZEN RIDGE =======================
    public static Level chapter3(Assets a) {
        Level L = new Level(3, "FROZEN RIDGE", "NORTHERN RIDGE  -  02:30", "DESTROY 3 UMBRA CONVOYS");
        Environment e = L.env;
        e.skyTop = new float[]{.01f, .02f, .07f}; e.skyHorizon = new float[]{.08f, .13f, .24f};
        e.sunDir = new float[]{.3f, -.7f, -.4f}; e.sunColor = new float[]{.55f, .8f, 1.5f};
        e.ambientSky = new float[]{.12f, .17f, .30f}; e.ambientGround = new float[]{.06f, .07f, .11f};
        e.fog = new float[]{.10f, .14f, .22f}; e.fogDensity = .014f; e.clouds = 0f; e.stars = 1f; e.aurora = 1f;
        e.exposure = 1.1f; e.bloom = .9f; e.saturation = 1.05f; e.ambient = Environment.Ambient.SNOW;
        L.half = 34f; L.groundTex = a.snow; L.groundScale = 6f; L.music = 3;
        L.ringSpawns(26f, 16);
        L.waves = new WaveSpec[]{new WaveSpec(6, 3, 1, 0), new WaveSpec(6, 5, 2, 0), new WaveSpec(8, 6, 2, 0)};
        L.intro = new Line[]{
            l("COMMAND", "Temperatures are minus thirty. Umbra's convoy is hauling reactor cores to a launch site up the mountain."),
            l("{HERO}", "Cold doesn't scare me. Let's cut that supply line."),
            l("COMMAND", "Expect heavy resistance - they won't give up the cores easily.")};
        L.outro = new Line[]{
            l("COMMAND", "Cores secured. But the Citadel is powering up - a fortress in the sky. This is our one shot."),
            l("{HERO}", "Then we finish this. Today.")};
        L.builder = w -> {
            Random rnd = new Random(31);
            for (int i = 0; i < 20; i++) {                                                  // cliff wall ring (solid)
                double ang = i * Math.PI * 2 / 20; float r = 40 + rnd.nextFloat() * 4, h = 12 + rnd.nextFloat() * 14;
                w.solid(a.cube, (float) Math.cos(ang) * r, 0, (float) Math.sin(ang) * r, 12 + rnd.nextFloat() * 4, h, 12 + rnd.nextFloat() * 4, .8f, .8f, .85f).tex(a.rock, 8f);
            }
            for (int i = 0; i < 70; i++) {                                                  // pine forest beyond the cliffs
                double ang = rnd.nextDouble() * Math.PI * 2; float r = 55 + rnd.nextFloat() * 55, h = 7 + rnd.nextFloat() * 8;
                float tx = (float) Math.cos(ang) * r, tz = (float) Math.sin(ang) * r;
                w.prop(a.cylinder, tx, 0, tz, .8f, h * .3f, .8f, .3f, .2f, .12f).noShadow();
                for (int k = 0; k < 3; k++)
                    w.prop(a.cone, tx, h * .25f + k * h * .22f, tz, h * (.55f - k * .12f), h * .4f, h * (.55f - k * .12f), .08f, .2f + k * .04f, .14f).noShadow();
            }
            for (int i = 0; i < 9; i++) {                                                   // ice pillars (cover)
                double ang = i * Math.PI * 2 / 9 + .2; float r = 11 + (i % 3) * 5;
                float h = 4 + (i % 4) * 1.5f;
                w.solid(a.cube, (float) Math.cos(ang) * r, 0, (float) Math.sin(ang) * r, 2.8f, h, 2.8f, .75f, .9f, 1f).tex(a.ice, 3f).emit(.25f).spec(.9f);
            }
            for (int i = 0; i < 6; i++) w.solid(a.cube, -20 + i * 8f, 0, -12 + (i % 2) * 24f, 4f, 1.1f, 3f, .95f, .97f, 1f).tex(a.snow, 3f);   // snow drifts (jumpable)
            w.solid(a.cube, 14, 0, -4, 8, 3, 3.2f, .25f, .3f, .22f).tex(a.plate, 3f);        // crashed supply truck
            w.prop(a.cube, 14, 3, -4, 3, 1.6f, 3.2f, .2f, .25f, .18f).tex(a.plate, 2f);
            for (int i = 0; i < 4; i++) {                                                   // fire barrels
                float bx = -8 + i * 6f, bz = i % 2 == 0 ? 6 : -8;
                w.solid(a.cylinder, bx, 0, bz, 1.1f, 1.4f, 1.1f, .5f, .15f, .1f).spec(.6f);
                w.prop(a.sphere, bx, 1.5f, bz, .9f, .9f, .9f, 1f, .5f, .1f).emit(4f).noShadow();
            }
        };
        return L;
    }

    // ======================= CHAPTER 4: THE CITADEL =======================
    public static Level chapter4(Assets a) {
        Level L = new Level(4, "THE CITADEL", "ORBITAL FORTRESS  -  ???", "DEFEAT WARDEN OMEGA");
        Environment e = L.env;
        e.skyTop = new float[]{.02f, .0f, .07f}; e.skyHorizon = new float[]{.38f, .08f, .5f};
        e.sunDir = new float[]{.25f, -.8f, .3f}; e.sunColor = new float[]{1.1f, .75f, 1.9f};
        e.ambientSky = new float[]{.22f, .16f, .38f}; e.ambientGround = new float[]{.09f, .05f, .13f};
        e.fog = new float[]{.14f, .05f, .22f}; e.fogDensity = .006f; e.clouds = 0f; e.stars = 1f; e.aurora = .7f;
        e.exposure = 1.1f; e.bloom = 1.2f; e.saturation = 1.15f; e.ambient = Environment.Ambient.SPARKS;
        L.half = 70f; L.voidLevel = true; L.music = 4; L.bossZ = -16f;
        L.ringSpawns(21f, 12);
        L.waves = new WaveSpec[]{new WaveSpec(4, 2, 0, 0), new WaveSpec(0, 0, 0, 2)};
        L.intro = new Line[]{
            l("OMEGA", "Welcome, defender. I am Warden Omega. Your war ends here."),
            l("{HERO}", "Not today."),
            l("COMMAND", "Careful - jump over his ground slams and the laser sweep. Hit him when he recovers!")};
        L.outro = new Line[]{
            l("COMMAND", "Omega is down - the Citadel is falling out of orbit. Get clear!"),
            l("{HERO}", "Mission accomplished. The city is safe."),
            l("COMMAND", "Outstanding work. All teams stand down. THE END.")};
        L.builder = w -> {
            w.solid(a.cube, 0, -3, 0, 60, 3, 60, .9f, .9f, 1f).tex(a.tech, 7f).spec(.5f);                 // main platform
            for (int s = -1; s <= 1; s += 2) {
                w.solid(a.cube, s * 17f, 0, 12, 8, 1.3f, 8, .5f, .55f, .8f).tex(a.tech, 4f);               // raised pads
                w.solid(a.cube, s * 17f, 0, -12, 8, 1.3f, 8, .5f, .55f, .8f).tex(a.tech, 4f);
                for (int z = -1; z <= 1; z += 2) {                                                         // corner pylons with energy rings
                    float px = s * 25f, pz = z * 25f;
                    w.solid(a.cube, px, 0, pz, 3, 11, 3, .35f, .3f, .5f).tex(a.plate, 3f).spec(.7f);
                    for (int k = 0; k < 3; k++) w.prop(a.cylinder, px, 3f + k * 3f, pz, 3.6f, .35f, 3.6f, .7f, .3f, 1f).emit(3.5f).noShadow();
                }
            }
            for (int i = 0; i < 28; i++) {                                                                  // glowing platform edge
                float t = -29f + i * 2.15f;
                w.prop(a.cube, t, 0, -29.6f, 1.2f, .15f, .3f, .6f, .3f, 1f).emit(4f).noShadow();
                w.prop(a.cube, t, 0, 29.6f, 1.2f, .15f, .3f, .6f, .3f, 1f).emit(4f).noShadow();
                w.prop(a.cube, -29.6f, 0, t, .3f, .15f, 1.2f, .6f, .3f, 1f).emit(4f).noShadow();
                w.prop(a.cube, 29.6f, 0, t, .3f, .15f, 1.2f, .6f, .3f, 1f).emit(4f).noShadow();
            }
            Random rnd = new Random(5);
            for (int i = 0; i < 26; i++) {                                                                  // floating debris
                double ang = rnd.nextDouble() * Math.PI * 2; float r = 45 + rnd.nextFloat() * 90, s = 6 + rnd.nextFloat() * 18;
                w.prop(a.cube, (float) Math.cos(ang) * r, -20 + rnd.nextFloat() * 50, (float) Math.sin(ang) * r, s, s * .6f, s, .5f, .5f, .6f).tex(a.rock, 6f).noShadow();
            }
            w.prop(a.sphere, 0, 70, -150, 60, 60, 60, .8f, .3f, 1f).emit(2.2f).noShadow();                  // giant energy core in the distance
            w.prop(a.cylinder, 0, 70, -150, 110, 1.2f, 110, .9f, .5f, 1f).emit(2f).noShadow();
        };
        return L;
    }

    // ======================= ARENA (endless survival) =======================
    public static Level arena(Assets a) {
        Level L = chapter1(a);
        Level A = new Level(0, "ARENA", "DOWNTOWN SURVIVAL", "SURVIVE AS LONG AS YOU CAN");
        A.env.skyTop = L.env.skyTop; A.env.skyHorizon = L.env.skyHorizon; A.env.sunDir = L.env.sunDir; A.env.sunColor = L.env.sunColor;
        A.env.ambientSky = L.env.ambientSky; A.env.ambientGround = L.env.ambientGround; A.env.fog = L.env.fog;
        A.env.fogDensity = L.env.fogDensity; A.env.clouds = L.env.clouds; A.env.stars = L.env.stars; A.env.ambient = L.env.ambient;
        A.env.exposure = L.env.exposure; A.env.bloom = L.env.bloom;
        A.half = L.half; A.groundTex = L.groundTex; A.groundScale = L.groundScale; A.music = 1; A.endless = true;
        A.ringSpawns(25f, 16);
        A.builder = L.builder;
        return A;
    }

    // ======================= STAGE (hero select / menus) =======================
    public static Level stage(Assets a) {
        Level L = new Level(-1, "STAGE", "", "");
        Environment e = L.env;
        e.skyTop = new float[]{.02f, .01f, .08f}; e.skyHorizon = new float[]{.30f, .08f, .40f};
        e.sunDir = new float[]{.5f, -.45f, -.7f}; e.sunColor = new float[]{2.6f, 2.3f, 2.6f};
        e.ambientSky = new float[]{.24f, .22f, .42f}; e.ambientGround = new float[]{.10f, .06f, .14f};
        e.fog = new float[]{.12f, .05f, .20f}; e.fogDensity = .008f; e.clouds = 0f; e.stars = 1f; e.aurora = .6f;
        e.exposure = 1.1f; e.bloom = 1.1f; e.ambient = Environment.Ambient.SPARKS;
        L.half = 20f; L.groundTex = a.tech; L.groundScale = 6f; L.music = 0;
        L.builder = w -> {
            w.prop(a.cylinder, 0, -.4f, 0, 11f, .4f, 11f, .6f, .65f, .9f).tex(a.tech, 4f).spec(.6f);
            w.prop(a.cylinder, 0, -.35f, 0, 11.6f, .1f, 11.6f, .5f, .3f, 1f).emit(3.5f).noShadow();
            for (int i = 0; i < 10; i++) {
                double ang = i * Math.PI * 2 / 10; float px = (float) Math.cos(ang) * 17f, pz = (float) Math.sin(ang) * 17f;
                float h = 6 + (i % 3) * 3;
                w.prop(a.cube, px, 0, pz, 1.6f, h, 1.6f, .25f, .25f, .35f).tex(a.plate, 3f);
                w.prop(a.cube, px, h * .3f, pz, 1.7f, .25f, 1.7f, .4f, .9f, 1f).emit(3.5f).noShadow();
            }
        };
        return L;
    }

    public static Level chapter(int n, Assets a) {
        return switch (n) { case 1 -> chapter1(a); case 2 -> chapter2(a); case 3 -> chapter3(a); default -> chapter4(a); };
    }
}
