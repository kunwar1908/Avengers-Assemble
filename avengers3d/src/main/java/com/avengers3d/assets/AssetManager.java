package com.avengers3d.assets;

import com.avengers3d.render.Mesh;
import com.avengers3d.render.MeshData;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * Two-stage streaming:
 *   1. WORKER threads: disk I/O + decoding (OBJ parse, STB image decode)  -> no GL context needed.
 *   2. GL thread: pump() runs queued uploads under a per-frame time budget -> no hitches.
 * OpenGL contexts are bound to one thread, so GPU uploads MUST happen on the render thread.
 */
public final class AssetManager implements AutoCloseable {
    private final ExecutorService workers = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "asset-loader"); t.setDaemon(true); return t;
    });
    private final Queue<Runnable> glTasks = new ConcurrentLinkedQueue<>();
    private final Map<String, CompletableFuture<Mesh>> meshes = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Integer>> textures = new ConcurrentHashMap<>();

    public CompletableFuture<Mesh> loadMesh(String path) {
        return meshes.computeIfAbsent(path, p -> {
            CompletableFuture<Mesh> future = new CompletableFuture<>();
            workers.execute(() -> {
                try {
                    MeshData data = ObjParser.parse(p);                       // worker thread
                    glTasks.add(() -> future.complete(new Mesh(data)));       // GL thread
                } catch (Exception e) { future.completeExceptionally(e); }
            });
            return future;
        });
    }

    public CompletableFuture<Integer> loadTexture(String path) {
        return textures.computeIfAbsent(path, p -> {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            workers.execute(() -> {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), c = stack.mallocInt(1);
                    ByteBuffer pixels = STBImage.stbi_load(p, w, h, c, 4);    // force RGBA
                    if (pixels == null) throw new IllegalStateException(STBImage.stbi_failure_reason());
                    int width = w.get(0), height = h.get(0);
                    glTasks.add(() -> {
                        int id = glGenTextures();
                        glBindTexture(GL_TEXTURE_2D, id);
                        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                        glGenerateMipmap(GL_TEXTURE_2D);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                        STBImage.stbi_image_free(pixels);                     // free native memory after upload
                        future.complete(id);
                    });
                } catch (Exception e) { future.completeExceptionally(e); }
            });
            return future;
        });
    }

    /** Call once per frame on the GL thread, e.g. pump(2_000_000) = 2 ms budget. */
    public void pump(long budgetNanos) {
        long deadline = System.nanoTime() + budgetNanos;
        Runnable task;
        while (System.nanoTime() < deadline && (task = glTasks.poll()) != null) task.run();
    }

    @Override public void close() { workers.shutdownNow(); }
}
