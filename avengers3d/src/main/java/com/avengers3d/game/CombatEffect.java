package com.avengers3d.game;

import com.avengers3d.render.Mesh;
import com.avengers3d.scene.GameObject3D;
import org.joml.Vector3f;

/** Short-lived procedural burst used for readable combat feedback. */
public final class CombatEffect extends GameObject3D {
    private float life;

    public CombatEffect(Mesh sphere, int texture, float radius, float r, float g, float b, float seconds) {
        textureId = texture;
        life = seconds;
        useGravity = false;
        flash = 0.65f;
        addPart(sphere, 0, radius, 0, radius, radius, radius, r, g, b);
    }

    public boolean updateEffect(float dt) {
        life -= dt;
        float scale = 1f + (0.22f - Math.max(0f, life)) * 5f;
        transform().setScale(scale, scale, scale);
        flash = Math.max(0f, life);
        return life > 0f;
    }

    @Override public void update(float dt) { updateEffect(dt); }
}
