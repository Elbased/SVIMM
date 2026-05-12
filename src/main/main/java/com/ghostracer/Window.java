package com.ghostracer;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {
    private long handle;
    private int width = 1280;
    private int height = 720;
    private boolean resized = false;

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("failed to init glfw");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        handle = glfwCreateWindow(width, height, "ghostracer // alpha", NULL, NULL);
        if (handle == NULL) throw new RuntimeException("failed to create window");

        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
            width = w;
            height = h;
            resized = true;
        });

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        glfwShowWindow(handle);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_MULTISAMPLE);
        glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public void swapAndPoll() { glfwSwapBuffers(handle); glfwPollEvents(); }
    public long getHandle() { return handle; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getAspect() { return (float) width / height; }

    public boolean wasResized() {
        if (resized) { resized = false; return true; }
        return false;
    }

    public void cleanup() {
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
