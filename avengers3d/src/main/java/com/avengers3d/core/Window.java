package com.avengers3d.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** GLFW window + OpenGL 3.3 core context. Must be created and used on the main thread. */
public final class Window implements AutoCloseable {
    private final long handle;
    private int width, height;
    private boolean resized;
    private final GLFWFramebufferSizeCallback sizeCallback;

    public Window(String title, int w, int h) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialise GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);   // required on macOS
        glfwWindowHint(GLFW_SAMPLES, 4);                          // 4x MSAA

        handle = glfwCreateWindow(w, h, title, NULL, NULL);
        if (handle == NULL) throw new IllegalStateException("Failed to create the window");

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);                                      // vsync: caps FPS to the monitor
        GL.createCapabilities();

        sizeCallback = GLFWFramebufferSizeCallback.create((win, fw, fh) -> { width = fw; height = fh; resized = true; });
        glfwSetFramebufferSizeCallback(handle, sizeCallback);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer a = stack.mallocInt(1), b = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, a, b);
            width = a.get(0); height = b.get(0);
        }
        glfwShowWindow(handle);
    }

    public long handle() { return handle; }
    public int width() { return width; }
    public int height() { return Math.max(1, height); }
    public float aspect() { return width / (float) height(); }
    public boolean consumeResize() { boolean r = resized; resized = false; return r; }
    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public void requestClose() { glfwSetWindowShouldClose(handle, true); }
    public void poll() { glfwPollEvents(); }
    public void swap() { glfwSwapBuffers(handle); }
    public void setCursorCaptured(boolean captured) {
        glfwSetInputMode(handle, GLFW_CURSOR, captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    @Override public void close() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
