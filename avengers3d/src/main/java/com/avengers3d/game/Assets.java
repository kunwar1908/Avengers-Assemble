package com.avengers3d.game;

import com.avengers3d.render.Mesh;
import com.avengers3d.render.ProcTextures;
import com.avengers3d.render.Textures;
import com.avengers3d.scene.Meshes;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

/** Every shared GPU resource the game needs: primitive meshes and procedural textures. */
public final class Assets implements AutoCloseable {
    public final Mesh cube, sphere, cylinder, cone;
    public final Meshes meshes;
    public final int white, asphalt, concrete, windows, plate, grate, snow, ice, rock, tech;

    public Assets() {
        cube = new Mesh(MeshFactory.cube());
        sphere = new Mesh(MeshFactory.sphere(14, 22));
        cylinder = new Mesh(MeshFactory.cylinder(24));
        cone = new Mesh(MeshFactory.cone(18));
        meshes = new Meshes(cube, sphere, cylinder, cone);

        white = Textures.solid(255, 255, 255);
        asphalt = ProcTextures.asphalt(256);
        concrete = ProcTextures.concrete(256);
        windows = ProcTextures.windows(256);
        plate = ProcTextures.plate(256);
        grate = ProcTextures.grate(256);
        snow = ProcTextures.snow(256);
        ice = ProcTextures.ice(256);
        rock = ProcTextures.rock(256);
        tech = ProcTextures.tech(256);
    }

    @Override public void close() {
        cube.close(); sphere.close(); cylinder.close(); cone.close();
        for (int t : new int[]{white, asphalt, concrete, windows, plate, grate, snow, ice, rock, tech}) glDeleteTextures(t);
    }
}
