package com.avengers3d.game;

import com.avengers3d.render.Mesh;
import com.avengers3d.scene.GameObject3D;

/** A static box-collider scenery object (platform, wall, building, crate...). Position = bottom centre. */
public final class Block extends GameObject3D {
    private final Part part;

    public Block(Mesh mesh, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        useGravity = false;
        halfExtents.set(sx / 2, sy / 2, sz / 2);
        transform.setPosition(x, y, z);
        part = addBox(mesh, 0, sy / 2, 0, sx, sy, sz, r, g, b);
        part.spec = 0.1f;
    }

    public Block emit(float e) { part.emissive = e; return this; }
    public Block tex(int id, float metresPerTile) { part.textureId = id; part.texScale = metresPerTile; return this; }
    public Block spec(float s) { part.spec = s; return this; }
    public Block noShadow() { castShadow = false; part.castShadow = false; return this; }

    @Override public void update(float dt) { }
}
