package com.avengers3d.render;

/** CPU-side mesh produced on worker threads (no GL calls). Uploaded to the GPU by Mesh on the GL thread. */
public record MeshData(float[] positions, float[] uvs, float[] normals, int[] indices) { }
