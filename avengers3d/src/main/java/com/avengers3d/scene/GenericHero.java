package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** Configurable original hero archetype used by the expanded roster. */
public final class GenericHero extends Character3D {
    public enum Kind {
        HAWKEYE("HAWKEYE", 105f, 8f, 0.65f, 0.35f, 0.2f),
        WEB_RUNNER("SPIDER-MAN", 110f, 9f, 0.8f, 0.15f, 0.25f),
        MYSTIC_WEAVER("SCARLET WITCH", 90f, 7f, 0.55f, 0.15f, 0.65f),
        VISIONARY("VISION", 125f, 7f, 0.35f, 0.8f, 0.75f),
        PANTHER("BLACK PANTHER", 120f, 9f, 0.08f, 0.08f, 0.1f),
        SIZE_SHIFTER("ANT-MAN", 100f, 7f, 0.85f, 0.55f, 0.15f),
        WINTER_SOLDIER("WINTER SOLDIER", 125f, 7f, 0.12f, 0.18f, 0.3f),
        ARCANE_SAGE("DOCTOR STRANGE", 95f, 7f, 0.45f, 0.15f, 0.8f),
        SOLAR_VANGUARD("CAPTAIN MARVEL", 140f, 8f, 0.9f, 0.35f, 0.08f);

        final String name;
        final float health, speed, r, g, b;

        Kind(String name, float health, float speed, float r, float g, float b) {
            this.name = name; this.health = health; this.speed = speed;
            this.r = r; this.g = g; this.b = b;
        }
    }

    private final Kind kind;

    public GenericHero(Kind kind, Mesh cube) {
        this.kind = kind;
        halfExtents.set(0.5f, 1f, 0.5f);
        maxHealth = health = kind.health;
        walkSpeed = kind.speed;
        addPart(cube, 0, 0.6f, 0, 0.65f, 1.1f, 0.48f, kind.r, kind.g, kind.b);
        addPart(cube, 0, 1.65f, 0, 0.82f, 0.95f, 0.55f, kind.r, kind.g, kind.b);
        addPart(cube, 0, 2.45f, 0, 0.48f, 0.48f, 0.48f, Math.min(1f, kind.r + 0.2f),
                Math.min(1f, kind.g + 0.2f), Math.min(1f, kind.b + 0.2f));
        addPart(cube, -0.58f, 1.65f, 0, 0.24f, 0.9f, 0.28f, kind.r, kind.g, kind.b);
        addPart(cube, 0.58f, 1.65f, 0, 0.24f, 0.9f, 0.28f, kind.r, kind.g, kind.b);
    }

    public Kind kind() { return kind; }
    @Override public String displayName() { return kind.name; }

    @Override public void update(float dt) {
        tickTimers(dt);
        if (knockTimer <= 0f) {
            velocity.x = moveInput.x * walkSpeed;
            velocity.z = moveInput.z * walkSpeed;
        }
        faceMoveDirection();
    }
}
