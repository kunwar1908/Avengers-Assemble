package com.avengers3d.game;

import com.avengers3d.render.Mesh;
import com.avengers3d.scene.GameObject3D;

/** Static solid box (platform / wall). Position = bottom centre. */
public final class Block extends GameObject3D {
    public Block(Mesh cube, int tex, float x, float y, float z, float sx, float sy, float sz,
                 float r, float g, float b) {
        useGravity = false;
        textureId = tex;
        halfExtents.set(sx / 2, sy / 2, sz / 2);
        transform.setPosition(x, y, z);
        addPart(cube, 0, sy / 2, 0, sx, sy, sz, r, g, b);
    }

    @Override public void update(float dt) { }
}
