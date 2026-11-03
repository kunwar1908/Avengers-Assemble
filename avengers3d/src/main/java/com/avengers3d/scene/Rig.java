package com.avengers3d.scene;

import com.avengers3d.render.Mesh;
import com.avengers3d.scene.GameObject3D.Part;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural humanoid skeleton + animator. Built from boxes, but every limb is a two-bone chain
 * (shoulder->elbow->hand, hip->knee->foot), so walking, jumping, flying and attacks are real joint rotations.
 *
 * Sign conventions (character faces +Z, its left side is +X):
 *   rotX > 0 swings a hanging limb BACKWARD and tilts a torso FORWARD;  rotX < 0 swings a limb forward/up.
 *   rotZ > 0 lifts the LEFT arm outward (right arm uses the negative).
 */
public final class Rig {
    public static final int ATK_NONE = 0, ATK_SWING = 1, ATK_SHOOT = 2, ATK_THROW = 3, ATK_SLAM = 4, ATK_CAST = 5, ATK_DASH = 6;

    /** Per-frame animation inputs (reused, so animating allocates nothing). */
    public static final class Input {
        public float speed, maxSpeed = 6f, vy, hit, attackT;
        public boolean grounded = true, flying;
        public int attack;
    }

    public Part pelvis, chest, head, cape, shL, shR, elL, elR, handL, handR, hipL, hipR, kneeL, kneeR;
    private final Look look;
    private final float k;
    private final List<Part> glow = new ArrayList<>();
    private final List<Float> glowBase = new ArrayList<>();
    private float phase, time;

    public Rig(GameObject3D o, Meshes m, Look look) {
        this.look = look;
        this.k = look.scale;
        build(o, m);
    }

    // ---- construction -----------------------------------------------------
    /** Add a box part. All positions and sizes are authored for a 1.8 m figure and multiplied by the look's scale. */
    private Part b(GameObject3D o, Mesh mesh, Part parent, float jx, float jy, float jz,
                   float ox, float oy, float oz, float sx, float sy, float sz, float[] c) {
        return o.addPart(mesh, parent, jx * k, jy * k, jz * k, ox * k, oy * k, oz * k, sx * k, sy * k, sz * k, c[0], c[1], c[2]);
    }

    private Part glowing(Part p, float emissive) {
        p.emissive = emissive; glow.add(p); glowBase.add(emissive);
        return p;
    }

    private void build(GameObject3D o, Meshes m) {
        Mesh cube = m.cube();
        float w = look.bulk;
        float[] foreC = look.forearm != null ? look.forearm : look.suit;
        float[] gloveC = look.glove != null ? look.glove : look.skin;
        float[] shinC = look.shin != null ? look.shin : look.suit2;

        pelvis = b(o, cube, null, 0, .95f, 0, 0, 0, 0, .34f * w, .20f, .22f * w, look.suit2);
        chest = b(o, cube, pelvis, 0, .10f, 0, 0, .27f, 0, .46f * w, .50f, .26f * w, look.suit);
        head = b(o, cube, chest, 0, .55f, 0, 0, .13f, 0, .21f, .24f, .23f, look.skin);

        if (look.helm != null) b(o, cube, head, 0, 0, 0, 0, .045f, -.02f, .245f, .19f, .245f, look.helm);
        if (look.hair != null) b(o, cube, head, 0, 0, 0, 0, .075f, -.035f, .235f, .12f, .26f, look.hair);
        if (look.bun) b(o, m.sphere(), head, 0, 0, 0, 0, .06f, -.15f, .10f, .10f, .10f, look.hair != null ? look.hair : look.suit);
        if (look.eyes) {
            for (int s = -1; s <= 1; s += 2)
                glowing(b(o, cube, head, 0, 0, 0, s * .05f, .015f, .118f, .05f, .028f, .02f, look.accent), 3.2f);
        }
        if (look.cape != null) cape = b(o, cube, chest, 0, .48f, -.13f * w, 0, -.33f, -.02f, .42f * w, .66f, .03f, look.cape);

        if (look.chestGlow) glowing(b(o, cube, chest, 0, 0, 0, 0, .33f, .135f * w, .11f, .11f, .03f, look.accent), 3.5f);
        if (look.star) {
            b(o, cube, chest, 0, 0, 0, 0, .30f, .134f * w, .17f, .17f, .02f, Look.c(.95f, .95f, .98f)).emissive = .35f;
        }
        if (look.quiver) {
            b(o, cube, chest, 0, 0, 0, .09f * w, .28f, -.17f, .09f, .42f, .09f, Look.c(.25f, .15f, .10f));
            b(o, cube, chest, 0, 0, 0, .09f * w, .52f, -.17f, .07f, .06f, .07f, look.accent);
        }
        if (look.belt) glowing(b(o, cube, pelvis, 0, 0, 0, 0, 0, .115f * w, .30f * w, .05f, .02f, look.accent), 1.6f);

        for (int s = 1; s >= -1; s -= 2) {                      // s = +1 left, -1 right
            Part sh = b(o, cube, chest, s * .29f * w, .44f, 0, 0, -.13f, 0, .14f * w, .30f, .14f * w, look.suit);
            Part el = b(o, cube, sh, 0, -.27f, 0, 0, -.13f, 0, .12f * w, .27f, .12f * w, foreC);
            Part hand = b(o, cube, el, 0, -.26f, 0, 0, -.04f, 0, .10f * w, .11f, .11f * w, gloveC);
            if (look.pad != null) b(o, cube, sh, 0, 0, 0, 0, .02f, 0, .21f * w, .09f, .21f * w, look.pad);
            if (look.spikes) b(o, m.cone(), sh, 0, 0, 0, 0, .13f, 0, .09f, .22f, .09f, look.pad != null ? look.pad : look.suit2);
            if (look.palmGlow) glowing(b(o, cube, hand, 0, 0, 0, 0, -.085f, 0, .075f, .03f, .075f, look.accent), 3f);

            Part hip = b(o, cube, pelvis, s * .11f * w, -.07f, 0, 0, -.20f, 0, .17f * w, .42f, .18f * w, look.suit2);
            Part knee = b(o, cube, hip, 0, -.40f, 0, 0, -.19f, 0, .15f * w, .39f, .16f * w, shinC);
            Part foot = b(o, cube, knee, 0, -.40f, 0, 0, -.03f, .05f, .14f * w, .09f, .26f * w, look.boot);
            if (look.bootGlow) glowing(b(o, cube, foot, 0, 0, 0, 0, 0, -.14f, .09f, .06f, .04f, look.accent), 3f);

            if (s > 0) { shL = sh; elL = el; handL = hand; hipL = hip; kneeL = knee; }
            else       { shR = sh; elR = el; handR = hand; hipR = hip; kneeR = knee; }
        }
        buildProp(o, m);
    }

    private void buildProp(GameObject3D o, Meshes m) {
        Mesh cube = m.cube();
        switch (look.prop) {
            case HAMMER -> {
                b(o, cube, handR, 0, -.05f, 0, 0, -.20f, 0, .045f, .46f, .045f, Look.c(.35f, .22f, .12f));
                Part hd = b(o, cube, handR, 0, -.05f, 0, 0, -.46f, 0, .27f, .15f, .15f, Look.c(.72f, .74f, .80f));
                hd.spec = .8f; hd.emissive = .12f;
            }
            case SHIELD -> {
                Part mount = o.addPart(null, elL, 0, -.12f * k, .11f * k, 0, 0, 0, 1, 1, 1, 0, 0, 0);
                mount.rotX = (float) (Math.PI / 2);              // cylinder axis (local Y) now points along +Z
                float[][] cols = {{.75f, .08f, .08f}, {.95f, .95f, .95f}, {.75f, .08f, .08f}, {.1f, .2f, .65f}, {.95f, .95f, .95f}};
                float[] rad = {.52f, .42f, .32f, .22f, .09f};
                for (int i = 0; i < 5; i++) {
                    Part d = o.addPart(m.cylinder(), mount, 0, 0, 0, 0, (.01f * i) * k, 0,
                                       rad[i] * k, .05f * k, rad[i] * k, cols[i][0], cols[i][1], cols[i][2]);
                    d.spec = .7f;
                }
            }
            case BOW -> {
                b(o, cube, handL, 0, -.04f, 0, 0, 0, .12f, .035f, .82f, .05f, Look.c(.30f, .18f, .35f));
                b(o, cube, handL, 0, -.04f, 0, 0, 0, .06f, .012f, .76f, .012f, Look.c(.9f, .9f, .9f));
            }
            case PISTOLS -> {
                b(o, cube, handL, 0, -.05f, 0, 0, -.02f, .10f, .06f, .08f, .22f, Look.c(.08f, .08f, .1f));
                b(o, cube, handR, 0, -.05f, 0, 0, -.02f, .10f, .06f, .08f, .22f, Look.c(.08f, .08f, .1f));
            }
            case BLADE -> glowing(b(o, cube, handR, 0, -.05f, 0, 0, -.02f, .52f, .05f, .08f, 1.0f, look.accent), 3.2f);
            case CANNONS -> {
                for (Part el : new Part[]{elL, elR}) {
                    b(o, cube, el, 0, 0, 0, 0, -.08f, .18f, .20f, .20f, .46f, Look.c(.18f, .18f, .22f));
                    glowing(b(o, cube, el, 0, 0, 0, 0, -.08f, .42f, .13f, .13f, .04f, look.accent), 3.5f);
                }
            }
            default -> { }
        }
    }

    /** Scale every glowing part's emissive value (enemy telegraphs, rage aura ...). */
    public void setGlow(float mult) {
        for (int i = 0; i < glow.size(); i++) glow.get(i).emissive = glowBase.get(i) * mult;
    }

    // ---- animation --------------------------------------------------------
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float sm(float x) { x = Math.max(0f, Math.min(1f, x)); return x * x * (3f - 2f * x); }
    private static float damp(float cur, float tgt, float rate, float dt) { return cur + (tgt - cur) * (1f - (float) Math.exp(-rate * dt)); }

    public void animate(Input in, float dt) {
        time += dt;
        boolean air = !in.grounded && !in.flying;
        float amp = (in.grounded && !in.flying) ? Math.min(1f, in.speed / Math.max(0.5f, in.maxSpeed)) : 0f;
        if (in.attack == ATK_DASH) amp = 1f;
        if (amp > 0.03f) phase += dt * (5f + in.speed * 1.05f);
        float s = (float) Math.sin(phase), c = (float) Math.cos(phase);

        // --- locomotion: legs swing, knees flex while the leg travels forward, arms counter-swing
        float hL = s * .85f * amp, hR = -s * .85f * amp;
        float kL = Math.max(0f, -c) * amp + .04f, kR = Math.max(0f, c) * amp + .04f;
        float aL = -s * .7f * amp, aR = s * .7f * amp;
        float eL = -(.22f + .5f * amp), eR = eL;
        float zL = .07f, zR = -.07f;
        float chX = .05f + .12f * amp, chY = s * .15f * amp, hdX = -chX * .6f;
        float bob = -Math.abs(c) * .05f * amp;
        float idle = (float) Math.sin(time * 2.1f);
        aL += idle * .03f * (1f - amp); aR -= idle * .03f * (1f - amp);

        if (in.flying) {
            hL = hR = .3f; kL = kR = .15f; aL = aR = .45f; zL = .25f; zR = -.25f; eL = eR = -.2f; chX = 0; hdX = 0; bob = 0;
        } else if (air) {
            if (in.vy > .5f) { hL = -.9f; kL = 1.1f; hR = .3f; kR = .5f; aL = aR = -.5f; zL = .6f; zR = -.6f; }
            else { hL = -.35f; hR = .35f; kL = kR = .25f; aL = aR = -.1f; zL = 1.0f; zR = -1.0f; }
        }
        if (look.prop == Look.Prop.SHIELD) { aL = -.55f; eL = -1.25f; zL = .1f; }     // guard stance
        if (look.prop == Look.Prop.PISTOLS && in.attack == ATK_NONE) { aL -= .25f; aR -= .25f; }

        // --- attack overlays
        float t = in.attackT;
        switch (in.attack) {
            case ATK_SWING -> {
                float a, tw, lean;
                if (t < .4f)       { float e = sm(t / .4f);          a = lerp(0, -2.9f, e);      lean = lerp(0, -.25f, e);   tw = lerp(0, .5f, e); }
                else if (t < .58f) { float e = sm((t - .4f) / .18f); a = lerp(-2.9f, -.35f, e);  lean = lerp(-.25f, .5f, e); tw = lerp(.5f, -.45f, e); }
                else               { float e = sm((t - .58f) / .42f); a = lerp(-.35f, 0, e);     lean = lerp(.5f, .05f, e);  tw = lerp(-.45f, 0, e); }
                aR = a; aL = a * .85f; eR = eL = -.25f; chX = lean; chY = tw; zL = .1f; zR = -.1f;
            }
            case ATK_SHOOT -> {
                float wgt = t < .75f ? 1f : 1f - (t - .75f) / .25f;
                float kick = t < .15f ? (1f - t / .15f) * .18f : 0f;
                aR = lerp(aR, -1.5f + kick, wgt); eR = lerp(eR, -.05f, wgt); zR = lerp(zR, -.05f, wgt);
                chY = lerp(chY, -.2f, wgt); chX = lerp(chX, -.03f - kick * .4f, wgt);
                if (look.prop == Look.Prop.PISTOLS) { aL = lerp(aL, -1.4f, wgt); eL = lerp(eL, -.1f, wgt); zL = lerp(zL, .05f, wgt); }
            }
            case ATK_THROW -> {
                float a, tw;
                if (t < .35f)      { float e = sm(t / .35f);         a = lerp(0, 1.4f, e);      tw = lerp(0, .6f, e); }
                else if (t < .6f)  { float e = sm((t - .35f) / .25f); a = lerp(1.4f, -1.7f, e); tw = lerp(.6f, -.5f, e); }
                else               { float e = sm((t - .6f) / .4f);   a = lerp(-1.7f, 0, e);    tw = lerp(-.5f, 0, e); }
                if (look.prop == Look.Prop.SHIELD) { aL = a; eL = -.2f; chY = -tw; } else { aR = a; eR = -.2f; chY = tw; }
            }
            case ATK_SLAM -> {
                float a, lean;
                if (t < .45f)      { float e = sm(t / .45f);          a = lerp(0, -2.9f, e);     lean = lerp(0, -.3f, e); }
                else if (t < .6f)  { float e = sm((t - .45f) / .15f); a = lerp(-2.9f, -.2f, e);  lean = lerp(-.3f, .7f, e); }
                else               { float e = sm((t - .6f) / .4f);   a = lerp(-.2f, 0, e);      lean = lerp(.7f, .05f, e); }
                aL = aR = a; eL = eR = -.15f; chX = lean; zL = .1f; zR = -.1f;
            }
            case ATK_CAST -> {
                float wgt = t < .2f ? sm(t / .2f) : (t < .8f ? 1f : 1f - (t - .8f) / .2f);
                zL = lerp(zL, 1.35f, wgt); zR = lerp(zR, -1.35f, wgt);
                aL = lerp(aL, -.3f, wgt); aR = lerp(aR, -.3f, wgt);
                eL = lerp(eL, -.1f, wgt); eR = lerp(eR, -.1f, wgt);
                chX = lerp(chX, -.18f, wgt); hdX = lerp(hdX, -.25f, wgt);
            }
            case ATK_DASH -> {
                chX = .6f; aL = aR = .9f; eL = eR = -.2f; hdX = -.3f;
                hL = s; hR = -s; kL = Math.max(0f, -c) * 1.2f + .2f; kR = Math.max(0f, c) * 1.2f + .2f;
            }
            default -> { }
        }
        chX -= .5f * in.hit; hdX -= .3f * in.hit; zL += .5f * in.hit; zR -= .5f * in.hit;   // hit recoil

        float rate = in.attack != ATK_NONE ? 36f : 16f;                  // snappier while attacking
        pelvis.offY = damp(pelvis.offY, bob * k, rate, dt);
        chest.rotX = damp(chest.rotX, chX, rate, dt);
        chest.rotY = damp(chest.rotY, chY, rate, dt);
        chest.vsy = 1f + .012f * idle * (1f - amp);                      // breathing
        head.rotX = damp(head.rotX, hdX, rate, dt);
        shL.rotX = damp(shL.rotX, aL, rate, dt); shL.rotZ = damp(shL.rotZ, zL, rate, dt); elL.rotX = damp(elL.rotX, eL, rate, dt);
        shR.rotX = damp(shR.rotX, aR, rate, dt); shR.rotZ = damp(shR.rotZ, zR, rate, dt); elR.rotX = damp(elR.rotX, eR, rate, dt);
        hipL.rotX = damp(hipL.rotX, hL, rate, dt); kneeL.rotX = damp(kneeL.rotX, kL, rate, dt);
        hipR.rotX = damp(hipR.rotX, hR, rate, dt); kneeR.rotX = damp(kneeR.rotX, kR, rate, dt);
        if (cape != null) {
            float tgt = .12f + Math.min(.9f, in.speed * .06f) + (air && in.vy < -2f ? .5f : 0f)
                      + (float) Math.sin(time * 6f + phase) * .04f * (amp + .2f);
            cape.rotX = damp(cape.rotX, tgt, 10f, dt);
        }
    }
}
