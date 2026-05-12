package com.ghostracer;

import static org.lwjgl.glfw.GLFW.*;

public class Input {
    private static final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] prevKeys = new boolean[GLFW_KEY_LAST + 1];
    private static double mouseX, mouseY, mouseDx, mouseDy;
    private static double prevMouseX, prevMouseY;
    private static boolean firstMouse = true;

    public static void init(long window) {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key <= GLFW_KEY_LAST) {
                keys[key] = action != GLFW_RELEASE;
            }
        });
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            mouseX = x;
            mouseY = y;
        });
    }

    public static void update() {
        System.arraycopy(keys, 0, prevKeys, 0, keys.length);
        if (firstMouse) {
            prevMouseX = mouseX;
            prevMouseY = mouseY;
            firstMouse = false;
        }
        mouseDx = mouseX - prevMouseX;
        mouseDy = mouseY - prevMouseY;
        prevMouseX = mouseX;
        prevMouseY = mouseY;
    }

    public static boolean isDown(int key) { return keys[key]; }
    public static boolean wasPressed(int key) { return keys[key] && !prevKeys[key]; }
    public static double getMouseDx() { return mouseDx; }
    public static double getMouseDy() { return mouseDy; }
}
