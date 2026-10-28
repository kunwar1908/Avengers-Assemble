package com.avengers3d.core;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Event-driven input state. key() = held, pressed() = went down since the last endFrame().
 * Call endFrame() once per frame after the game has read the input.
 */
public final class Input {
    private final boolean[] down = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] pressed = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] mouseDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private double lastX, lastY, dx, dy, scroll;
    private boolean haveLast;
    private final StringBuilder text = new StringBuilder();

    // Keep strong references to the native callbacks so they are never collected/freed early.
    private GLFWKeyCallback keyCb;
    private GLFWCursorPosCallback posCb;
    private GLFWMouseButtonCallback buttonCb;
    private GLFWScrollCallback scrollCb;
    private GLFWCharCallback charCb;

    public void attach(long window) {
        keyCb = GLFWKeyCallback.create((w, key, scancode, action, mods) -> {
            if (key < 0 || key > GLFW_KEY_LAST) return;
            if (action == GLFW_PRESS) { down[key] = true; pressed[key] = true; }
            else if (action == GLFW_RELEASE) down[key] = false;
        });
        posCb = GLFWCursorPosCallback.create((w, x, y) -> {
            if (haveLast) { dx += x - lastX; dy += y - lastY; }
            lastX = x; lastY = y; haveLast = true;
        });
        buttonCb = GLFWMouseButtonCallback.create((w, button, action, mods) -> {
            if (button < 0 || button > GLFW_MOUSE_BUTTON_LAST) return;
            if (action == GLFW_PRESS) { mouseDown[button] = true; mousePressed[button] = true; }
            else if (action == GLFW_RELEASE) mouseDown[button] = false;
        });
        scrollCb = GLFWScrollCallback.create((w, xo, yo) -> scroll += yo);
        charCb = GLFWCharCallback.create((w, codepoint) -> {
            if (Character.isLetterOrDigit(codepoint) || codepoint == ' ' || codepoint == '_' || codepoint == '-') {
                text.appendCodePoint(codepoint);
            }
        });
        glfwSetKeyCallback(window, keyCb);
        glfwSetCursorPosCallback(window, posCb);
        glfwSetMouseButtonCallback(window, buttonCb);
        glfwSetScrollCallback(window, scrollCb);
        glfwSetCharCallback(window, charCb);
    }

    public boolean key(int k) { return down[k]; }
    public boolean pressed(int k) { return pressed[k]; }
    public boolean mouseDown(int b) { return mouseDown[b]; }
    public boolean mousePressed(int b) { return mousePressed[b]; }
    public double mouseDX() { return dx; }
    public double mouseDY() { return dy; }
    public double scroll() { return scroll; }
    public String text() { return text.toString(); }

    /** Forget the last cursor position (call when the cursor mode changes, avoids a view jump). */
    public void resetMouse() { haveLast = false; dx = 0; dy = 0; }

    public void endFrame() {
        Arrays.fill(pressed, false);
        Arrays.fill(mousePressed, false);
        dx = 0; dy = 0; scroll = 0;
        text.setLength(0);
    }
}
