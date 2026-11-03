package com.avengers3d.game;

import com.avengers3d.scene.GameObject3D;
import com.avengers3d.scene.Meshes;
import org.joml.Vector3f;

/** Pooled projectile: hero bolts, arrows, the ricocheting shield, and enemy shots. */
public final class Projectile extends GameObject3D {
    public enum Style { REPULSOR, TRACER, ARROW, EXPLOSIVE, SHIELD, ENEMY, MISSILE }

    private static final float R = 0.3f;

    public boolean active, hostile;
    public Style style = Style.REPULSOR;
    public float life, damage, explodeRadius;
    public int pierce, bounces, lastHitId = -1;
    public float cr, cg, cb;                       // glow colour (also the dynamic light colour)

    private final Part glow, arrow, mount;
    private final Part[] shieldDiscs = new Part[5];

    public Projectile(Meshes m) {
        useGravity = false;
        castShadow = false;
        halfExtents.set(R, R, R);
        glow = addPart(m.sphere(), null, 0, R, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1);
        glow.castShadow = false;
        arrow = addPart(m.cube(), null, 0, R, 0, 0, 0, 0, .07f, .07f, 1.0f, .35f, .25f, .2f);
        arrow.castShadow = false;
        mount = addPart(null, null, 0, R, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0);
        mount.rotX = (float) (Math.PI / 2);
        float[][] cols = {{.75f, .08f, .08f}, {.95f, .95f, .95f}, {.75f, .08f, .08f}, {.1f, .2f, .65f}, {.95f, .95f, .95f}};
        float[] rad = {.62f, .50f, .38f, .26f, .11f};
        for (int i = 0; i < 5; i++) {
            shieldDiscs[i] = addPart(m.cylinder(), mount, 0, 0, 0, 0, .012f * i, 0, rad[i], .06f, rad[i], cols[i][0], cols[i][1], cols[i][2]);
            shieldDiscs[i].castShadow = false;
            shieldDiscs[i].spec = .7f;
        }
    }

    public void fire(Style s, Vector3f origin, Vector3f dir, float speed, float dmg, boolean enemy) {
        style = s; hostile = enemy; damage = dmg; life = 2.4f; active = true;
        pierce = 0; bounces = 0; explodeRadius = 0; lastHitId = -1;
        switch (s) {
            case REPULSOR -> { cr = .5f; cg = 1.7f; cb = 2.6f; setLook(false, false, .55f); }
            case TRACER -> { cr = 2.8f; cg = 2.0f; cb = .5f; setLook(false, false, .24f); }
            case ARROW -> { cr = 1.5f; cg = .6f; cb = 2.6f; setLook(false, true, .22f); pierce = 1; }
            case EXPLOSIVE -> { cr = 3f; cg = 1.2f; cb = .3f; setLook(false, true, .55f); explodeRadius = 6f; }
            case SHIELD -> { cr = .6f; cg = .8f; cb = 2.2f; setLook(true, false, 0f); bounces = 3; life = 3f; }
            case ENEMY -> { cr = 3f; cg = .35f; cb = .25f; setLook(false, false, .5f); life = 3.5f; }
            case MISSILE -> { cr = 3f; cg = .8f; cb = .3f; setLook(false, false, .75f); life = 4f; }
        }
        glow.r = cr / 3f; glow.g = cg / 3f; glow.b = cb / 3f; glow.emissive = 3f;
        transform.setPosition(origin.x, origin.y - R, origin.z);
        setDirection(dir);
        velocity.set(dir).mul(speed);
    }

    private void setLook(boolean shield, boolean arrowBody, float size) {
        glow.visible = !shield;
        glow.vsx = glow.vsy = glow.vsz = size;
        arrow.visible = arrowBody;
        for (Part d : shieldDiscs) d.visible = shield;
    }

    public void setDirection(Vector3f d) {
        float yaw = (float) Math.atan2(d.x, d.z);
        float pitch = (float) -Math.asin(Math.max(-1f, Math.min(1f, d.y / Math.max(1e-4f, d.length()))));
        transform.setEuler(pitch, yaw, 0f);
    }

    @Override public void update(float dt) {
        life -= dt;
        transform.translate(velocity.x * dt, velocity.y * dt, velocity.z * dt);
        if (style == Style.SHIELD) mount.rotY += dt * 22f;           // whirl like a thrown coin
        if (life <= 0f) active = false;
    }
}
