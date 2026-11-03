package com.avengers3d.scene;

import com.avengers3d.render.Mesh;

/** The shared primitive meshes every object is built from. */
public record Meshes(Mesh cube, Mesh sphere, Mesh cylinder, Mesh cone) { }
