package com.avengers3d.game;

import com.avengers3d.audio.AudioManager.Sound;
import com.avengers3d.scene.Look;
import com.avengers3d.scene.Rig;
import org.joml.Vector3f;

import static com.avengers3d.scene.Look.c;

/** The six playable heroes. Each has a distinct attack and special; stats and looks live right here as data. */
public final class Heroes {
    private Heroes() { }

    public static final HeroDef[] ALL = {ironMan(), hulk(), thor(), captain(), widow(), hawkeye()};

    private static HeroDef ironMan() {
        HeroDef d = new HeroDef("ironman", "IRON MAN");
        d.tagline = "ARMOURED GENIUS";
        d.attackName = "REPULSOR"; d.specialName = "UNIBEAM";
        d.perks = new String[]{"F: TOGGLE FLIGHT (SPACE UP / SHIFT DOWN)", "RAPID-FIRE PLASMA BOLTS", "UNIBEAM PIERCES EVERY ENEMY IN LINE"};
        d.health = 100; d.walkSpeed = 7f; d.jumpSpeed = 11f; d.attackCd = .16f; d.specialCd = 7f; d.canFly = true;
        d.theme = new float[]{1f, .35f, .25f};
        d.attackAnim = Rig.ATK_SHOOT; d.attackTime = .22f; d.specialAnim = Rig.ATK_CAST; d.specialTime = .7f;
        Look l = d.look;
        l.suit = c(.72f, .06f, .07f); l.suit2 = c(.72f, .06f, .07f); l.shin = c(.82f, .64f, .17f); l.forearm = c(.82f, .64f, .17f);
        l.skin = c(.85f, .66f, .18f); l.glove = c(.82f, .64f, .17f); l.helm = c(.72f, .06f, .07f); l.boot = c(.72f, .06f, .07f);
        l.accent = c(.5f, .95f, 1f);
        l.eyes = true; l.chestGlow = true; l.palmGlow = true; l.bootGlow = true;
        d.attack = (w, h, cam) -> {
            w.fireBolt(h, cam, Projectile.Style.REPULSOR, 62f, 12f, 0f);
            w.sfx(Sound.SHOOT, h);
        };
        d.special = (w, h, cam) -> {
            w.fireBeam(h, cam, 85f, 48f, 1.4f);
            w.sfx(Sound.BEAM, h);
            w.addShake(.7f);
        };
        return d;
    }

    private static HeroDef hulk() {
        HeroDef d = new HeroDef("hulk", "HULK");
        d.tagline = "THE STRONGEST ONE THERE IS";
        d.attackName = "SMASH"; d.specialName = "RAGE";
        d.perks = new String[]{"DOUBLE JUMP", "CTRL IN AIR: GROUND POUND SHOCKWAVE", "RAGE: +80% DAMAGE, +25% SPEED FOR 8 s"};
        d.health = 170; d.walkSpeed = 6.5f; d.jumpSpeed = 13f; d.attackCd = .55f; d.specialCd = 14f; d.airJumps = 1; d.groundPound = true;
        d.theme = new float[]{.35f, .9f, .3f};
        d.attackAnim = Rig.ATK_SWING; d.attackTime = .55f; d.specialAnim = Rig.ATK_CAST; d.specialTime = .8f;
        Look l = d.look;
        l.scale = 1.5f; l.bulk = 1.5f;
        l.suit = c(.28f, .68f, .22f); l.suit2 = c(.36f, .20f, .50f); l.shin = c(.28f, .68f, .22f); l.skin = c(.28f, .68f, .22f);
        l.forearm = c(.28f, .68f, .22f); l.glove = c(.28f, .68f, .22f); l.boot = c(.28f, .68f, .22f); l.hair = c(.05f, .04f, .06f);
        l.eyes = false; l.accent = c(.7f, 1f, .5f);
        d.attack = (w, h, cam) -> {
            int n = w.meleeArc(h, 2.2f, 3.6f, 36f, 12f);
            w.sfx(Sound.SMASH, h);
            if (n > 0) { w.addShake(.5f); w.hitStop(.05f); }
        };
        d.special = (w, h, cam) -> {
            h.rageTimer = 8f;
            Vector3f p = h.transform().position();
            w.shockwave(p, 7f, 20f, 16f);
            w.fx.ring(p.x, p.y, p.z, 7f, .6f, 3f, .5f);
            w.sfx(Sound.ROAR, h);
            w.addShake(.9f);
        };
        return d;
    }

    private static HeroDef thor() {
        HeroDef d = new HeroDef("thor", "THOR");
        d.tagline = "GOD OF THUNDER";
        d.attackName = "MJOLNIR"; d.specialName = "STORM";
        d.perks = new String[]{"HEAVY HAMMER ARC", "STORM: LIGHTNING STRIKES 7 ENEMIES", "AIR JUMP"};
        d.health = 140; d.walkSpeed = 6.8f; d.jumpSpeed = 12f; d.attackCd = .7f; d.specialCd = 10f; d.airJumps = 1;
        d.theme = new float[]{.45f, .65f, 1f};
        d.attackAnim = Rig.ATK_SWING; d.attackTime = .6f; d.specialAnim = Rig.ATK_CAST; d.specialTime = .8f;
        Look l = d.look;
        l.scale = 1.12f; l.bulk = 1.2f; l.prop = Look.Prop.HAMMER;
        l.suit = c(.16f, .18f, .26f); l.suit2 = c(.2f, .2f, .25f); l.skin = c(.93f, .76f, .62f); l.forearm = c(.6f, .62f, .68f);
        l.glove = c(.6f, .62f, .68f); l.boot = c(.3f, .2f, .12f); l.hair = c(.9f, .75f, .35f); l.cape = c(.7f, .08f, .1f);
        l.pad = c(.7f, .72f, .78f); l.accent = c(.5f, .7f, 1f);
        d.attack = (w, h, cam) -> {
            int n = w.meleeArc(h, 2.4f, 4.0f, 50f, 14f);
            w.sfx(Sound.SMASH, h);
            if (n > 0) { w.addShake(.45f); w.hitStop(.05f); }
        };
        d.special = (w, h, cam) -> {
            w.lightningStorm(h, 7, 30f, 95f);
            w.sfx(Sound.THUNDER, h);
            w.addShake(.8f);
        };
        return d;
    }

    private static HeroDef captain() {
        HeroDef d = new HeroDef("captain", "CAPTAIN AMERICA");
        d.tagline = "THE FIRST AVENGER";
        d.attackName = "SHIELD THROW"; d.specialName = "CHARGE";
        d.perks = new String[]{"SHIELD RICOCHETS BETWEEN 3 ENEMIES", "CHARGE: INVULNERABLE DASH THAT HITS HARD", "BALANCED AND DURABLE"};
        d.health = 130; d.walkSpeed = 7.2f; d.jumpSpeed = 11.5f; d.attackCd = .55f; d.specialCd = 8f;
        d.theme = new float[]{.3f, .5f, 1f};
        d.attackAnim = Rig.ATK_THROW; d.attackTime = .5f; d.specialAnim = Rig.ATK_DASH; d.specialTime = .42f;
        Look l = d.look;
        l.scale = 1.05f; l.bulk = 1.1f; l.prop = Look.Prop.SHIELD; l.star = true;
        l.suit = c(.12f, .22f, .62f); l.suit2 = c(.10f, .18f, .50f); l.skin = c(.93f, .76f, .62f); l.forearm = c(.12f, .22f, .62f);
        l.glove = c(.45f, .22f, .10f); l.boot = c(.45f, .10f, .10f); l.helm = c(.12f, .22f, .62f); l.accent = c(1f, 1f, 1f);
        d.attack = (w, h, cam) -> {
            w.fireBolt(h, cam, Projectile.Style.SHIELD, 38f, 30f, -.25f);
            w.sfx(Sound.SHIELD, h);
        };
        d.special = (w, h, cam) -> {
            Vector3f f = cam.flatForward(new Vector3f());
            h.startDash(f, 26f, .42f);
            h.iFrames = .45f;
            h.dashDamage = 45f;
            w.sfx(Sound.DODGE, h);
        };
        return d;
    }

    private static HeroDef widow() {
        HeroDef d = new HeroDef("widow", "BLACK WIDOW");
        d.tagline = "SUPER SPY";
        d.attackName = "DUAL PISTOLS"; d.specialName = "BLACK-OUT";
        d.perks = new String[]{"FASTEST FIRE RATE", "BLACK-OUT: STUNS ENEMIES 3.5 s", "FASTEST RUNNER"};
        d.health = 90; d.walkSpeed = 8.4f; d.jumpSpeed = 11.5f; d.attackCd = .11f; d.specialCd = 11f; d.airJumps = 1;
        d.theme = new float[]{1f, .75f, .2f};
        d.attackAnim = Rig.ATK_SHOOT; d.attackTime = .2f; d.specialAnim = Rig.ATK_CAST; d.specialTime = .6f;
        Look l = d.look;
        l.scale = .96f; l.bulk = .88f; l.prop = Look.Prop.PISTOLS; l.bun = true; l.belt = true;
        l.suit = c(.06f, .06f, .08f); l.suit2 = c(.06f, .06f, .08f); l.skin = c(.93f, .76f, .64f); l.glove = c(.06f, .06f, .08f);
        l.boot = c(.05f, .05f, .06f); l.hair = c(.62f, .12f, .05f); l.accent = c(1f, .75f, .2f);
        d.attack = (w, h, cam) -> {
            float lat = h.fireSide * .28f;
            h.fireSide = -h.fireSide;
            w.fireBolt(h, cam, Projectile.Style.TRACER, 78f, 7f, lat);
            w.sfx(Sound.SHOOT, h);
        };
        d.special = (w, h, cam) -> {
            w.stunAround(h.transform().position(), 10f, 3.5f, 18f);
            w.sfx(Sound.STUN, h);
            w.addShake(.4f);
        };
        return d;
    }

    private static HeroDef hawkeye() {
        HeroDef d = new HeroDef("hawkeye", "HAWKEYE");
        d.tagline = "NEVER MISSES";
        d.attackName = "ARROW"; d.specialName = "EXPLOSIVE ARROW";
        d.perks = new String[]{"PIERCING ARROWS", "EXPLOSIVE ARROW: 6 m BLAST", "LONG-RANGE SPECIALIST"};
        d.health = 100; d.walkSpeed = 7.4f; d.jumpSpeed = 12f; d.attackCd = .42f; d.specialCd = 7f; d.airJumps = 1;
        d.theme = new float[]{.8f, .4f, 1f};
        d.attackAnim = Rig.ATK_SHOOT; d.attackTime = .3f; d.specialAnim = Rig.ATK_SHOOT; d.specialTime = .4f;
        Look l = d.look;
        l.scale = 1.02f; l.prop = Look.Prop.BOW; l.quiver = true;
        l.suit = c(.30f, .12f, .45f); l.suit2 = c(.16f, .10f, .24f); l.skin = c(.9f, .72f, .58f); l.helm = c(.25f, .10f, .38f);
        l.glove = c(.12f, .08f, .16f); l.boot = c(.10f, .08f, .13f); l.accent = c(.9f, .5f, 1f);
        d.attack = (w, h, cam) -> {
            w.fireBolt(h, cam, Projectile.Style.ARROW, 95f, 24f, 0f);
            w.sfx(Sound.ARROW, h);
        };
        d.special = (w, h, cam) -> {
            w.fireBolt(h, cam, Projectile.Style.EXPLOSIVE, 70f, 40f, 0f);
            w.sfx(Sound.ARROW, h);
        };
        return d;
    }
}
